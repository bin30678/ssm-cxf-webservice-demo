package com.example.cxfdemo.interceptor;

import com.example.cxfdemo.utils.WebUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import java.io.InputStream;

public class AuditRequestInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger log = LoggerFactory.getLogger(AuditRequestInterceptor.class);
    public static final String AUDIT_GUID = "AUDIT_GUID";

    @Resource
    private com.example.cxfdemo.dao.AuditLogDao auditLogDao;

    public void setAuditLogDao(com.example.cxfdemo.dao.AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    private com.example.cxfdemo.dao.AuditLogDao getEffectiveDao() {
        if (this.auditLogDao != null) {
            return this.auditLogDao;
        }
        try {
            WebApplicationContext ctx = ContextLoader.getCurrentWebApplicationContext();
            if (ctx != null) {
                this.auditLogDao = ctx.getBean(com.example.cxfdemo.dao.AuditLogDao.class);
            }
        } catch (Exception e) {
            log.warn("Failed to get AuditLogDao from Spring context: {}", e.getMessage());
        }
        return this.auditLogDao;
    }

    public AuditRequestInterceptor() {
        super(Phase.RECEIVE);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        HttpServletRequest request = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
        
        String guid = null;
        Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        if (headers != null && headers.containsKey("guid")) {
            List<String> guidList = headers.get("guid");
            if (guidList != null && !guidList.isEmpty()) {
                guid = guidList.get(0);
            }
        }
        
        if (guid == null || guid.trim().isEmpty()) {
            guid = UUID.randomUUID().toString();
        }
        
        message.getExchange().put(AUDIT_GUID, guid);

        String ip = WebUtils.getClientIp(request);
        String clientType = WebUtils.getClientType(request);
        String hostname = WebUtils.getLocalHostname();
        String uri = (String) message.get(Message.REQUEST_URI);
        String method = (String) message.get(Message.HTTP_REQUEST_METHOD);

        // 讀取並保留 Request Body Payload
        String payload = null;
        try {
            InputStream is = message.getContent(InputStream.class);
            if (is != null) {
                CachedOutputStream cos = new CachedOutputStream();
                IOUtils.copy(is, cos);
                cos.flush();
                byte[] bytes = cos.getBytes();
                if (bytes != null && bytes.length > 0) {
                    payload = new String(bytes, "UTF-8");
                }
                // 重設 InputStream 讓 CXF 框架能繼續讀取
                message.setContent(InputStream.class, cos.getInputStream());
                cos.close();
            }
        } catch (Exception e) {
            log.warn("Failed to capture request payload: {}", e.getMessage());
        }

        if ((payload == null || payload.trim().isEmpty()) && request != null) {
            String qs = request.getQueryString();
            if (qs != null && !qs.trim().isEmpty()) {
                payload = "?" + qs;
            }
        }

        log.info("=== [Audit Request] ===");
        log.info("GUID: {}", guid);
        log.info("Time: {}", new Date());
        log.info("Client IP: {}", ip);
        log.info("Client Type: {}", clientType);
        log.info("Host: {}", hostname);
        log.info("URI: {} {}", method, uri);
        if (payload != null) {
            log.info("Payload: {}", payload.length() > 500 ? payload.substring(0, 500) + "..." : payload);
        }
        log.info("=======================");
        
        com.example.cxfdemo.dao.AuditLogDao dao = getEffectiveDao();
        if (dao != null) {
            dao.insertRequestLog(guid, ip, clientType, hostname, uri, method, payload);
        } else {
            log.warn("AuditLogDao is not injected or found!");
        }
    }
}
