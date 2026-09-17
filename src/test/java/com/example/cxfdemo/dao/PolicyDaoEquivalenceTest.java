package com.example.cxfdemo.dao;

import com.example.cxfdemo.model.PolicyInfo;
import com.example.cxfdemo.service.MainDbOperationsService;
import com.external.liba.dao.ExternalJarGenericDao;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * SSM 與 Boot 等價性驗證測試：
 * 依據最新需求，由真正載入 applicationContext.xml 建立 Spring 容器。
 * 1. 斷言 Spring 容器僅暴露單一主 DataSource Bean (dataSource1)。
 * 2. 斷言 BaseDao 三個欄位 (sqlSessionTemplate, jdbcTemplate, dataSource) 均由 XML 正確注入主庫元件。
 * 3. 斷言 MainDbOperationsService 四種存取方式（Mapper、Template、JdbcTemplate、原生 JDBC）讀取同一保單欄位完全一致。
 * 4. 斷言跨四種方式在 @Transactional 交易中原子回滾與正常提交。
 * 5. 斷言 GenericDao 四個 JNDI 路徑（完整與相對名稱）及兩段式子 Context (java:comp/env) 查找。
 * 6. 斷言 DemoImpDao 透過 JNDI lookup (jdbc/cxfdemo2) 操作 DB2，與主庫隔離。
 * 7. 斷言外部 JAR 內部 lookup 模擬 (ExternalJarGenericDao) 正常運作且具備自主交易邊界。
 * 8. 斷言外部 XML 存在於 Classpath 但未被主專案載入。
 */
public class PolicyDaoEquivalenceTest {

    private JdbcDataSource ds1;
    private JdbcDataSource ds2;
    private JdbcDataSource ds3;
    private JdbcDataSource ds4;

    private ClassPathXmlApplicationContext context;
    private PolicyDaoImpl policyDao;
    private MainDbOperationsService mainDbOperationsService;
    private DemoImpDao demoImpDao;

    @Before
    public void setUp() throws Exception {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.example.cxfdemo.dao.MockInitialContextFactory");

        // 建立 4 個隔離的 H2 資料庫 (cxfdemo1, cxfdemo2, as400_a, as400_b)
        ds1 = new JdbcDataSource();
        ds1.setURL("jdbc:h2:mem:ssm_main_" + System.nanoTime() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        ds1.setUser("sa");
        ds1.setPassword("");

        ds2 = new JdbcDataSource();
        ds2.setURL("jdbc:h2:mem:ssm_db2_" + System.nanoTime() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        ds2.setUser("sa");
        ds2.setPassword("");

        ds3 = new JdbcDataSource();
        ds3.setURL("jdbc:h2:mem:ssm_as400a_" + System.nanoTime() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        ds3.setUser("sa");
        ds3.setPassword("");

        ds4 = new JdbcDataSource();
        ds4.setURL("jdbc:h2:mem:ssm_as400b_" + System.nanoTime() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        ds4.setUser("sa");
        ds4.setPassword("");

        // 初始化 ds1 (主庫): policy_info, system_properties, audit 表
        JdbcTemplate jt1 = new JdbcTemplate(ds1);
        jt1.execute("CREATE TABLE IF NOT EXISTS system_properties (prop_key VARCHAR(64) PRIMARY KEY, prop_value VARCHAR(256))");
        jt1.execute("INSERT INTO system_properties VALUES ('system.app.url', 'http://localhost:8080/cxfdemo')");
        jt1.execute("CREATE TABLE IF NOT EXISTS policy_info (policy_no VARCHAR(64) PRIMARY KEY, holder_name VARCHAR(128), product_name VARCHAR(128), status VARCHAR(32))");
        jt1.execute("INSERT INTO policy_info VALUES ('POL-SSM-001', '張大千', '長青尊榮終身險', 'ACTIVE')");
        jt1.execute("CREATE TABLE IF NOT EXISTS audit_request (guid VARCHAR(64) PRIMARY KEY, client_ip VARCHAR(64), client_type VARCHAR(64), hostname VARCHAR(64), request_uri VARCHAR(256), request_method VARCHAR(16), payload TEXT)");
        jt1.execute("CREATE TABLE IF NOT EXISTS audit_response (guid VARCHAR(64) PRIMARY KEY, response_code INT, payload TEXT)");
        jt1.execute("CREATE TABLE IF NOT EXISTS audit_fault (guid VARCHAR(64) PRIMARY KEY, error_msg TEXT)");

        // 初始化 ds2 (外部庫): mock_table_db2, customers
        JdbcTemplate jt2 = new JdbcTemplate(ds2);
        jt2.execute("CREATE TABLE IF NOT EXISTS mock_table_db2 (id INT PRIMARY KEY, name VARCHAR(64))");
        jt2.execute("INSERT INTO mock_table_db2 VALUES (1, 'DB2-Record-1')");
        jt2.execute("CREATE TABLE IF NOT EXISTS customers (id INT AUTO_INCREMENT PRIMARY KEY, customer_name VARCHAR(64), status VARCHAR(32))");
        jt2.execute("INSERT INTO customers (customer_name, status) VALUES ('Customer_DB2_A', 'ACTIVE')");
        jt2.execute("INSERT INTO customers (customer_name, status) VALUES ('Customer_DB2_INACTIVE', 'INACTIVE')");

        // 初始化 ds3 (AS400-A): customers
        JdbcTemplate jt3 = new JdbcTemplate(ds3);
        jt3.execute("CREATE TABLE IF NOT EXISTS customers (id INT AUTO_INCREMENT PRIMARY KEY, customer_name VARCHAR(64), status VARCHAR(32))");
        jt3.execute("INSERT INTO customers (customer_name, status) VALUES ('Customer_AS400A_1', 'ACTIVE')");

        // 初始化 ds4 (AS400-B): customers
        JdbcTemplate jt4 = new JdbcTemplate(ds4);
        jt4.execute("CREATE TABLE IF NOT EXISTS customers (id INT AUTO_INCREMENT PRIMARY KEY, customer_name VARCHAR(64), status VARCHAR(32))");
        jt4.execute("INSERT INTO customers (customer_name, status) VALUES ('Customer_AS400B_1', 'ACTIVE')");

        // 綁定 4 個 JNDI 資源 (完整路徑與相對路徑)
        MockInitialContextFactory.clear();
        MockInitialContextFactory.bind("jdbc/cxfdemo1", ds1);
        MockInitialContextFactory.bind("java:comp/env/jdbc/cxfdemo1", ds1);
        MockInitialContextFactory.bind("jdbc/cxfdemo2", ds2);
        MockInitialContextFactory.bind("java:comp/env/jdbc/cxfdemo2", ds2);
        MockInitialContextFactory.bind("jdbc/as400_a", ds3);
        MockInitialContextFactory.bind("java:comp/env/jdbc/as400_a", ds3);
        MockInitialContextFactory.bind("jdbc/as400_b", ds4);
        MockInitialContextFactory.bind("java:comp/env/jdbc/as400_b", ds4);

        // 真正載入正式 applicationContext.xml 建立 Spring 容器
        context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");

        policyDao = (PolicyDaoImpl) context.getBean("policyDao");
        mainDbOperationsService = context.getBean(MainDbOperationsService.class);
        demoImpDao = context.getBean(DemoImpDao.class);
    }

    @After
    public void tearDown() {
        if (context != null) {
            context.close();
        }
        MockInitialContextFactory.clear();
    }

    @Test
    public void testSinglePrimaryDataSourceBeanInSpringContext() {
        // 斷言 Spring 容器中僅有 1 個 DataSource Bean (dataSource1)
        Map<String, DataSource> dsBeans = context.getBeansOfType(DataSource.class);
        assertEquals("Spring 容器應僅暴露 1 個 DataSource Bean", 1, dsBeans.size());
        assertTrue(context.containsBean("dataSource1"));
        assertFalse(context.containsBean("dataSource2"));
    }

    @Test
    public void testBaseDaoFieldsInjectedFromXml() {
        // 斷言 BaseDao 三個欄位均由 XML 正確注入主庫元件
        assertNotNull("sqlSessionTemplate 必須注入", policyDao.getSqlSessionTemplate());
        assertNotNull("jdbcTemplate 必須注入", policyDao.getJdbcTemplate());
        assertNotNull("dataSource 必須注入", policyDao.getDataSource());
        assertSame("dataSource 必須為主庫 dataSource1", context.getBean("dataSource1"), policyDao.getDataSource());
    }

    @Test
    public void testFourOperationsReadSameInitialData() {
        // 透過 MainDbOperationsService 四種方式查詢同一保單
        PolicyInfo p1 = mainDbOperationsService.findViaMapper("POL-SSM-001");
        PolicyInfo p2 = mainDbOperationsService.findViaSqlSessionTemplate("POL-SSM-001");
        PolicyInfo p3 = mainDbOperationsService.findViaJdbcTemplate("POL-SSM-001");
        PolicyInfo p4 = mainDbOperationsService.findViaPureJdbc("POL-SSM-001");

        assertNotNull(p1);
        assertNotNull(p2);
        assertNotNull(p3);
        assertNotNull(p4);

        assertEquals("張大千", p1.getHolderName());
        assertEquals(p1.getHolderName(), p2.getHolderName());
        assertEquals(p1.getHolderName(), p3.getHolderName());
        assertEquals(p1.getHolderName(), p4.getHolderName());

        assertEquals("長青尊榮終身險", p1.getProductName());
        assertEquals(p1.getProductName(), p2.getProductName());
        assertEquals(p1.getProductName(), p3.getProductName());
        assertEquals(p1.getProductName(), p4.getProductName());

        assertEquals("ACTIVE", p1.getStatus());
        assertEquals(p1.getStatus(), p2.getStatus());
        assertEquals(p1.getStatus(), p3.getStatus());
        assertEquals(p1.getStatus(), p4.getStatus());
    }

    @Test
    public void testMixedOperationsRollbackOnException() {
        // 混合四種方式在同一個 @Transactional 中寫入，異常時全數原子回滾
        PolicyInfo p1 = new PolicyInfo("TX-001", "張三", "壽險A", "ACTIVE");
        PolicyInfo p2 = new PolicyInfo("TX-002", "李四", "壽險B", "ACTIVE");
        PolicyInfo p3 = new PolicyInfo("TX-003", "王五", "壽險C", "ACTIVE");
        PolicyInfo p4 = new PolicyInfo("TX-004", "趙六", "壽險D", "ACTIVE");

        try {
            mainDbOperationsService.executeMixedFourOperations(p1, p2, p3, p4, true);
            fail("應拋出模擬異常");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Simulated transaction rollback exception"));
        }

        // 驗證四筆紀錄全數回滾，主庫查無紀錄
        assertNull(mainDbOperationsService.findViaMapper("TX-001"));
        assertNull(mainDbOperationsService.findViaSqlSessionTemplate("TX-002"));
        assertNull(mainDbOperationsService.findViaJdbcTemplate("TX-003"));
        assertNull(mainDbOperationsService.findViaPureJdbc("TX-004"));
    }

    @Test
    public void testMixedOperationsCommitOnSuccessAndCanReadEachOther() {
        // 混合四種方式在同一個 @Transactional 中寫入，正常提交
        PolicyInfo p1 = new PolicyInfo("OK-001", "劉備", "蜀漢險", "ACTIVE");
        PolicyInfo p2 = new PolicyInfo("OK-002", "關羽", "青龍險", "ACTIVE");
        PolicyInfo p3 = new PolicyInfo("OK-003", "張飛", "蛇矛險", "ACTIVE");
        PolicyInfo p4 = new PolicyInfo("OK-004", "諸葛亮", "八陣圖險", "ACTIVE");

        mainDbOperationsService.executeMixedFourOperations(p1, p2, p3, p4, false);

        // 交叉讀取驗證提交成果與資料一致性
        assertNotNull(mainDbOperationsService.findViaPureJdbc("OK-001"));
        assertNotNull(mainDbOperationsService.findViaJdbcTemplate("OK-002"));
        assertNotNull(mainDbOperationsService.findViaSqlSessionTemplate("OK-003"));
        assertNotNull(mainDbOperationsService.findViaMapper("OK-004"));

        assertEquals("劉備", policyDao.findPolicyViaMapper("OK-001").getHolderName());
        assertEquals("關羽", policyDao.findPolicyViaJdbc("OK-002").getHolderName());
        assertEquals("張飛", policyDao.findPolicyViaJdbc("OK-003").getHolderName());
        assertEquals("諸葛亮", policyDao.findPolicyViaMapper("OK-004").getHolderName());
    }

    @Test
    public void testGenericDaoFourJndiConnectionsAndSubcontext() throws Exception {
        // 驗證 GenericDao 四個連線路徑
        Connection c1 = GenericDao.getConnection1();
        Connection c2 = GenericDao.getConnection2();
        Connection c3 = GenericDao.getConnection3();
        Connection c4 = GenericDao.getConnection4();

        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(c3);
        assertNotNull(c4);

        assertTrue(c1.getMetaData().getURL().contains("ssm_main_"));
        assertTrue(c2.getMetaData().getURL().contains("ssm_db2_"));
        assertTrue(c3.getMetaData().getURL().contains("ssm_as400a_"));
        assertTrue(c4.getMetaData().getURL().contains("ssm_as400b_"));

        c1.close();
        c2.close();
        c3.close();
        c4.close();

        // 驗證兩段式子 Context 查找 (lookup("java:comp/env") -> lookup("jdbc/cxfdemo2"))
        Context rootCtx = new InitialContext();
        Context envCtx = (Context) rootCtx.lookup("java:comp/env");
        assertNotNull("java:comp/env 子 Context 應存在", envCtx);

        DataSource dsLookup = (DataSource) envCtx.lookup("jdbc/cxfdemo2");
        assertNotNull("兩段式查找 jdbc/cxfdemo2 應成功", dsLookup);
        assertSame(ds2, dsLookup);
    }

    @Test
    public void testDemoImpDaoUsesJndiDb2AndIsolatedFromMainDb() {
        // 驗證 DemoImpDao 透過 JNDI lookup (jdbc/cxfdemo2) 正常操作 DB2
        List<Map<String, Object>> list = demoImpDao.queryUsingSqlSessionTemplate();
        assertNotNull(list);
        assertEquals(1, list.size());
        Object recordName = list.get(0).get("name") != null ? list.get(0).get("name") : list.get(0).get("NAME");
        assertEquals("DB2-Record-1", recordName);

        List<Map<String, Object>> jdbcList = demoImpDao.queryUsingJdbcTemplate();
        assertEquals(1, jdbcList.size());

        List<String> pureList = demoImpDao.queryUsingPureJdbc();
        assertEquals(1, pureList.size());
        assertEquals("DB2-Record-1", pureList.get(0));

        // 斷言 DB2 查詢與操作不干擾主庫 policy_info
        JdbcTemplate jt1 = new JdbcTemplate(ds1);
        int mainCount = jt1.queryForObject("SELECT COUNT(*) FROM policy_info", Integer.class);
        assertEquals(1, mainCount);
    }

    @Test
    public void testExternalJarInternalLookupAndTransactionIndependence() throws Exception {
        // 驗證 CodeSource 為真實 JAR 檔案，而非目錄或 src/test
        java.net.URL codeSourceLocation = ExternalJarGenericDao.class.getProtectionDomain().getCodeSource().getLocation();
        assertNotNull("CodeSource 不應為 null", codeSourceLocation);
        assertTrue("ExternalJarGenericDao 必須來自真實 JAR 檔案: " + codeSourceLocation,
                codeSourceLocation.toString().endsWith(".jar") || codeSourceLocation.toString().contains(".jar!"));

        // 驗證 Spring 容器透過 component-scan 成功將 JAR 內的 @Repository 掃描並註冊為 Bean
        assertTrue("Spring 容器必須成功註冊 externalJarGenericDao Bean", context.containsBean("externalJarGenericDao"));
        ExternalJarGenericDao externalJarDao = context.getBean("externalJarGenericDao", ExternalJarGenericDao.class);
        assertNotNull(externalJarDao);

        // 1. 內部 lookup 查詢活躍客戶
        List<String> activeList = externalJarDao.queryActiveCustomersViaInternalLookup("java:comp/env/jdbc/cxfdemo2");
        assertNotNull(activeList);
        assertTrue(activeList.contains("Customer_DB2_A"));

        // 2. 內部 lookup 交易邊界回滾驗證
        externalJarDao.insertCustomerViaInternalLookup("java:comp/env/jdbc/cxfdemo2", "TestExtRollback", false);
        JdbcTemplate jt2 = new JdbcTemplate(ds2);
        int countRollback = jt2.queryForObject("SELECT COUNT(*) FROM customers WHERE customer_name = 'TestExtRollback'", Integer.class);
        assertEquals("外部 JAR rollback 紀錄不應存在", 0, countRollback);

        // 3. 內部 lookup 交易邊界提交驗證
        externalJarDao.insertCustomerViaInternalLookup("java:comp/env/jdbc/cxfdemo2", "TestExtCommit", true);
        int countCommit = jt2.queryForObject("SELECT COUNT(*) FROM customers WHERE customer_name = 'TestExtCommit'", Integer.class);
        assertEquals("外部 JAR commit 紀錄應正常寫入", 1, countCommit);

        // 4. 驗證 AS400-A 與 AS400-B 內部 lookup
        List<String> as400AList = externalJarDao.queryActiveCustomersViaInternalLookup("java:comp/env/jdbc/as400_a");
        assertTrue(as400AList.contains("Customer_AS400A_1"));

        List<String> as400BList = externalJarDao.queryActiveCustomersViaInternalLookup("java:comp/env/jdbc/as400_b");
        assertTrue(as400BList.contains("Customer_AS400B_1"));
    }

    @Test
    public void testExternalXmlNotLoadedInSsmContext() throws Exception {
        // 1. 先確認實際 JAR 內確實包含 applicationContext-liba.xml
        java.io.InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("applicationContext-liba.xml");
        assertNotNull("外部 JAR 必須確實包含 applicationContext-liba.xml", xmlStream);

        // 讀取 XML 內容並確認確實定義了 externalXmlExclusiveBean 與同名覆蓋 Bean
        String xmlContent;
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(xmlStream, java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            xmlContent = sb.toString();
        }
        assertTrue("外部 XML 必須確實定義了 externalXmlExclusiveBean", xmlContent.contains("externalXmlExclusiveBean"));
        assertTrue("外部 XML 必須確實定義了同名覆蓋 Bean dataSource1", xmlContent.contains("dataSource1"));

        // 2. 斷言外部 XML (applicationContext-liba/b.xml) 未被 SSM Spring 容器載入
        assertFalse("外部 XML 專屬 Bean 絕不應存在於 SSM Spring 容器中", context.containsBean("externalXmlExclusiveBean"));

        // 3. 斷言同名 Bean 並未被外部 XML 覆蓋為 String 類型，仍為主專案定義之真實 DataSource
        Object ds1Bean = context.getBean("dataSource1");
        assertTrue("主專案 dataSource1 絕不應被外部同名 XML 覆蓋為 String", ds1Bean instanceof javax.sql.DataSource);
    }
}
