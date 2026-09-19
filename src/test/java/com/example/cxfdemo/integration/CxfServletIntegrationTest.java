package com.example.cxfdemo.integration;

import com.example.cxfdemo.config.ConfigSingleton;
import com.example.cxfdemo.dao.MockInitialContextFactory;
import com.example.cxfdemo.service.MailService;
import com.example.cxfdemo.service.MailServiceImpl;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.naming.Context;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

/** Runs requests through the real CXF/Dispatcher servlets and production XML, with H2 and temporary files. */
public class CxfServletIntegrationTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private MockServletContext servletContext;
    private XmlWebApplicationContext root;
    private CXFServlet cxf;
    private DispatcherServlet mvc;
    private JdbcTemplate jdbc;
    private File uploadDir;
    private String previousJndiFactory;

    @Before
    public void startApplication() throws Exception {
        previousJndiFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, MockInitialContextFactory.class.getName());
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:cxf_http_" + System.nanoTime() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        jdbc = new JdbcTemplate(dataSource);
        // Keep the in-memory database alive until the application closes.
        dataSource.setURL(dataSource.getURL() + ";DB_CLOSE_DELAY=-1");
        MockInitialContextFactory.bind("jdbc/cxfdemo1", dataSource);
        uploadDir = temporary.newFolder("uploads");
        jdbc.execute("CREATE TABLE system_properties (prop_key VARCHAR(100) PRIMARY KEY, prop_value VARCHAR(255))");
        jdbc.update("INSERT INTO system_properties VALUES (?, ?)", "file.upload.dir", uploadDir.getAbsolutePath());
        jdbc.update("INSERT INTO system_properties VALUES (?, ?)", "file.upload.max-size", "16");
        jdbc.update("INSERT INTO system_properties VALUES (?, ?)", "mail.host", "smtp.test.invalid");
        jdbc.update("INSERT INTO system_properties VALUES (?, ?)", "mail.port", "2525");
        jdbc.execute("CREATE TABLE policy_info (policy_no VARCHAR(64) PRIMARY KEY, holder_name VARCHAR(128), product_name VARCHAR(128), status VARCHAR(32))");
        jdbc.execute("INSERT INTO policy_info VALUES ('P-TEST', 'Test Holder', 'Test Product', 'ACTIVE')");
        jdbc.execute("CREATE TABLE file_upload_record (id BIGINT AUTO_INCREMENT PRIMARY KEY, original_name VARCHAR(255), stored_name VARCHAR(255), content_type VARCHAR(100), size_bytes BIGINT, description VARCHAR(255), created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE audit_request (guid VARCHAR(64) PRIMARY KEY, client_ip VARCHAR(64), client_type VARCHAR(64), hostname VARCHAR(64), request_uri VARCHAR(256), request_method VARCHAR(16), payload TEXT)");
        jdbc.execute("CREATE TABLE audit_response (guid VARCHAR(64) PRIMARY KEY, response_code INT, payload TEXT)");
        jdbc.execute("CREATE TABLE audit_fault (guid VARCHAR(64) PRIMARY KEY, error_msg TEXT)");
        servletContext = new MockServletContext();
        root = new XmlWebApplicationContext();
        root.setServletContext(servletContext);
        root.setConfigLocation("classpath:applicationContext.xml");
        // No scheduled filesystem cleanup or background jobs during request tests.
        root.addBeanFactoryPostProcessor(factory -> {
            for (String name : factory.getBeanDefinitionNames()) {
                String type = factory.getBeanDefinition(name).getBeanClassName();
                if ("org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor".equals(type)) {
                    ((org.springframework.beans.factory.support.BeanDefinitionRegistry) factory).removeBeanDefinition(name);
                } else if ("org.springframework.scheduling.quartz.SchedulerFactoryBean".equals(type)) {
                    factory.getBeanDefinition(name).getPropertyValues().add("autoStartup", false);
                }
            }
        });
        root.refresh();
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, root);
        cxf = new CXFServlet();
        cxf.init(new MockServletConfig(servletContext, "CXFServlet"));
        mvc = new DispatcherServlet();
        MockServletConfig mvcConfig = new MockServletConfig(servletContext, "SpringMVC");
        mvcConfig.addInitParameter("contextConfigLocation", "classpath:spring-mvc.xml");
        mvc.init(mvcConfig);
    }

    @After
    public void stopApplication() {
        if (mvc != null) mvc.destroy();
        if (cxf != null) cxf.destroy();
        if (root != null) root.close();
        if (jdbc != null) jdbc.execute("SHUTDOWN");
        MockInitialContextFactory.clear();
        if (previousJndiFactory == null) System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
        else System.setProperty(Context.INITIAL_CONTEXT_FACTORY, previousJndiFactory);
    }

    @Test
    public void healthAndJsonInquiryRunThroughCxfInterceptors() throws Exception {
        MockHttpServletResponse health = call("GET", "/rest", "/health", null, null);
        assertEquals(200, health.getStatus());
        assertEquals("UP", json(health).get("status").getAsString());
        assertHeaders(health);
        MockHttpServletResponse inquiry = call("POST", "/rest", "/policies/inquiry", "application/json", "{\"policyNo\":\"P-TEST\"}");
        assertEquals(200, inquiry.getStatus());
        assertEquals("ACTIVE", json(inquiry).getAsJsonObject("policy").get("status").getAsString());
        assertHeaders(inquiry);
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM audit_request", Integer.class) > 0);
    }

    @Test
    public void restRejectsUnsupportedMediaTypeAndKeepsJsonError() throws Exception {
        MockHttpServletResponse response = call("POST", "/rest", "/policies/inquiry", "text/plain", "invalid");
        assertEquals(415, response.getStatus());
        assertEquals("UNSUPPORTED_MEDIA_TYPE", json(response).get("code").getAsString());
        assertEquals("integration-trace-123", json(response).get("traceId").getAsString());
        assertHeaders(response);
    }

    @Test
    public void multipartUploadStillWritesFileAndMetadata() throws Exception {
        MockHttpServletResponse response = upload("hello");
        assertEquals(response.getContentAsString(), 200, response.getStatus());
        JsonObject body = json(response);
        assertEquals("SUCCESS", body.get("status").getAsString());
        File stored = new File(uploadDir, body.get("storedName").getAsString());
        assertEquals("hello", new String(Files.readAllBytes(stored.toPath()), StandardCharsets.UTF_8));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject("SELECT COUNT(*) FROM file_upload_record", Integer.class));
        assertHeaders(response);
    }

    @Test
    public void uploadLimitComesFromDatabaseAndLeavesNoPartialFile() throws Exception {
        MockHttpServletResponse response = upload("01234567890123456789");
        assertEquals(500, response.getStatus()); // Existing API contract; size failures are currently returned as 500.
        assertEquals("FAILED", json(response).get("status").getAsString());
        assertTrue(json(response).get("message").getAsString().contains("16 bytes"));
        assertEquals(0, uploadDir.list().length);
        assertEquals(Integer.valueOf(0), jdbc.queryForObject("SELECT COUNT(*) FROM file_upload_record", Integer.class));
    }

    @Test
    public void soapSuccessAndFaultKeepHeadersAndStructuredError() throws Exception {
        String envelope = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body>"
                + "<p:getPolicyStatus xmlns:p=\"https://example.com/cxfdemo/policy\"><policyNo>P-TEST</policyNo></p:getPolicyStatus>"
                + "</s:Body></s:Envelope>";
        MockHttpServletResponse ok = call("POST", "/Webservice", "/soap/policies", "text/xml; charset=UTF-8", envelope);
        assertEquals(ok.getContentAsString(), 200, ok.getStatus());
        assertTrue(ok.getContentAsString().contains("ACTIVE"));
        assertHeaders(ok);
        MockHttpServletResponse fault = call("POST", "/Webservice", "/soap/policies", "application/json", "{}");
        assertEquals(fault.getContentAsString(), 415, fault.getStatus());
        assertTrue(fault.getContentAsString().contains("ServiceError"));
        assertTrue(fault.getContentAsString().contains("UNSUPPORTED_MEDIA_TYPE"));
        assertTrue(fault.getContentAsString().contains("integration-trace-123"));
        assertHeaders(fault);
    }

    @Test
    public void springMvcDoesNotRunCxfValidationOrResponseHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "POST", "/tasks/clean-backup");
        request.setServletPath("/tasks/clean-backup");
        request.setContentType("application/x-www-form-urlencoded");
        request.setParameter("dir", new File(temporary.getRoot(), "missing").getAbsolutePath());
        MockHttpServletResponse response = new MockHttpServletResponse();
        mvc.service(request, response);
        assertEquals(200, response.getStatus());
        assertFalse(json(response).get("success").getAsBoolean());
        assertNull(response.getHeader("X-Request-Id"));
        assertNull(response.getHeader("X-Content-Type-Options"));
    }

    @Test
    public void mailServiceBuildsSenderFromDbWithoutXmlMailSender() throws Exception {
        assertFalse(root.containsBean("mailSender"));
        assertFalse(root.containsBean("javaMailService"));
        MailService service = root.getBean(MailService.class);
        assertTrue(service instanceof MailServiceImpl);
        ConfigSingleton config = ConfigSingleton.getInstance();
        config.reload();
        try {
            Method create = MailServiceImpl.class.getDeclaredMethod("createMailSender");
            create.setAccessible(true);
            JavaMailSenderImpl sender = (JavaMailSenderImpl) create.invoke(service);
            assertEquals("smtp.test.invalid", sender.getHost());
            assertEquals(2525, sender.getPort());
        } finally {
            config.clear();
        }
    }

    private MockHttpServletResponse upload(String text) throws Exception {
        String body = "--test-boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"sample.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n" + text + "\r\n--test-boundary--\r\n";
        return call("POST", "/rest", "/files/upload", "multipart/form-data; boundary=test-boundary", body);
    }

    private MockHttpServletResponse call(String method, String servletPath, String path, String type, String body) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext, method, servletPath + path);
        request.setServletPath(servletPath);
        request.setPathInfo(path);
        request.addHeader("X-Request-Id", "integration-trace-123");
        if (type != null) { request.setContentType(type); request.addHeader("Content-Type", type); }
        // Real servlet containers provide an empty stream for bodyless requests.
        request.setContent(body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        cxf.service(request, response);
        return response;
    }

    private JsonObject json(MockHttpServletResponse response) throws Exception {
        return new JsonParser().parse(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private void assertHeaders(MockHttpServletResponse response) {
        assertEquals("integration-trace-123", response.getHeader("X-Request-Id"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
    }
}
