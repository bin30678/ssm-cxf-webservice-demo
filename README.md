# SSM + Apache CXF WebService 企業級示範專案 (ssm-cxf-webservice-demo)

傳統 WAR 架構（非 Spring Boot），基於 **Java 8** 整合 Spring 3.2、Spring MVC、MyBatis 3.3、Apache CXF 3.0 JAX-RS / JAX-WS、Quartz 排程、外部模組連線控管與審計日誌。

---

## 核心技術版本

* **JDK**：Java 1.8 (Java 8)
* **Spring Framework**：3.2.14.RELEASE
* **Apache CXF**：3.0.6 (支援 JAX-RS 2.0 與 JAX-WS，使用標準 `javax.*` 命名空間)
* **MyBatis**：3.3.0（MyBatis-Spring 1.2.3）
* **Log4j2**：2.17.2
* **Web 容器**：Tomcat 6 / 7 / 8 / 9 (相容 Servlet 2.5 規範)

---

## 系統架構與服務端點 (Endpoints)

本系統整合了 **Apache CXF Servlet** 與 **Spring MVC DispatcherServlet**：

### 1. CXF RESTful API (`/rest/*`)
由 `CXFServlet` 映射 `/rest/*`，服務路徑定義於 `cxf-bus.xml` 與各 JAX-RS Resource：

* `GET  /rest/health`：系統健康檢查（回傳狀態與時間戳記）
* `GET  /rest/policies`：保單清單查詢（支援 CXF Audit 攔截器 Response 延遲緩衝寫入 DB）
* `POST /rest/policies`：建立新保單
* `POST /rest/policies/inquiry`：保單明細查詢
* `GET  /rest/policies/test-async-bank`：觸發非同步背景 20 次銀行長輪詢重試服務
* `GET  /rest/policies/test-external-jar`：觸發外部模組（Lib-A / Lib-B）連線傳遞與主專案事務控管
* `GET  /rest/policies/test-rest-template`：觸發 HttpClient 4.5 信任憑證繞過與外部 API 日誌記錄
* `GET  /rest/policies/test-clean-backup`：CXF 觸發清理過期備份檔案
* `POST /rest/files/upload`：Multipart 檔案上傳與安全路徑過濾

### 2. CXF SOAP WebService (`/ws/*`)
由 `CXFServlet` 映射 `/ws/*`，端點設定於 `cxf-bus.xml`：

* **SOAP 端點位址**：`http://localhost:8080/ws/soap/policies`
* **WSDL 定義位址**：`http://localhost:8080/ws/soap/policies?wsdl`
* **服務定義**：`PolicyInquiryService`（實作類別：`PolicyInquiryPortImpl`），提供 `getPolicyStatus` 與 `getPolicySummary` 兩組 Operations。

### 3. Spring MVC 控制器 (`/`)
由 `DispatcherServlet` 映射 `/`，提供即時手動排程管理：

* `GET  /tasks/clean-backup`：手動觸發排程清理過期備份檔（支援參數：`dir` 預設 `C:/backup_folder`，`days` 預設 7 天）
* `POST /tasks/clean-backup`：同上，支援 POST 呼叫

---

## 關鍵商業邏輯與架構特色

1. **資料庫設定動態載入 (`DatabasePropertyPlaceholderConfigurer`)**：
   - 繼承 Spring `PropertyPlaceholderConfigurer`。
   - 容器啟動時透過 `beanFactory` 取得主資料庫 `DataSource` (`dataSource1`) 建立連線。
   - 自資料表 `system_properties` 動態讀取系統設定（如 `system.app.url`），並由 `MethodInvokingFactoryBean` 同步注入至靜態類別 `GlobalConfig`。
2. **系統啟動自動體檢 (`ServletContextListener`)**：
   - `TiffImageReaderCheckListener`：自動檢測 JVM 環境是否具備 TIFF ImageReader。
   - `FontCheckListener`：自動檢測作業系統是否安裝指定字型（預設：標楷體）。
3. **主庫持久層架構 (`BaseDao` + `PolicyDaoImpl`)**：
   - `BaseDao` 由 XML 注入主庫三大元件（`SqlSessionTemplate`、`JdbcTemplate`、`DataSource`）。
   - `MainDbOperationsService` 提供四種資料庫存取方式（Mapper、SqlSessionTemplate、JdbcTemplate、原生 JDBC），在 `@Transactional` 中保證跨方式操作之一致性與原子回滾。
4. **外部 JAR 模組連線控管 (`MainProjectIntegrationService`)**：
   - 整合外部 `external-lib-a`（Service+DAO）與 `external-lib-b`（純 DAO）。
   - 所有外部模組皆由主專案建立與傳遞 `Connection`，連線之 Commit、Rollback 及最終 Close 完全由主專案掌握。
5. **安全與審計攔截器**：
   - `AuditRequestInterceptor`、`AuditResponseInterceptor`、`AuditFaultInterceptor`：利用 `CacheAndWriteOutputStream` 確保 Response 在 Stream 關閉時完整收集 Payload 並寫入審計日誌。
   - `UnifiedExceptionMapper`：統一處理 REST 例外格式。
6. **非同步長輪詢與檔案備份清理**：
   - `AsyncBankTransferService`：支援最多 20 次重試之後台非同步銀行傳送任務。
   - `ScheduledTasks` & `TaskController`：支援 Spring 定時排程與透過 HTTP GET/POST 立即觸發過期備份清理。

---

## 建置與自動化測試

本專案配置完整的單元測試與整合驗證套件，執行：

```powershell
mvn clean test
```

### 測試覆蓋統計
目前共 **17 個測試類別、52 個測試案例，100% 全數通過**：
* `PolicyDaoEquivalenceTest` (9 tests)：驗證主庫單一暴露、BaseDao 三元件注入、四種存取一致性、事務原子性、四路 JNDI Lookup 與外部 JAR 內部 lookup 隔離。
* `GenericDaoTest` (4 tests)：驗證 JNDI Lookup 與容錯路徑。
* `RequestValidationInterceptorTest` (5 tests)：驗證 Content-Type 白名單與 StAX/XXE 防護。
* `UnifiedExceptionMapperTest` (4 tests)：驗證例外統一格式轉換。
* `PolicyInquiryPortImplTest` (3 tests)：驗證 SOAP WebService 業務操作。
* `MainProjectIntegrationServiceTest` (3 tests)：驗證外部模組連線傳遞與異常關閉。
* `FileStorageServiceTest` (3 tests)：驗證路徑安全檢核與檔案儲存。
* `AsyncBankTransferServiceTest` (1 test)：驗證背景 20 次重試機制。
* `ScheduledTasksTest` (1 test)：驗證過期備份遞迴清除邏輯。
* `RestTemplateUtilsTest` (1 test)：驗證 SSL 繞過與 API 日誌寫入。
* `TaskControllerTest` (1 test)：驗證 Spring MVC 即時清理控制器。
* `HealthResourceTest` (1 test)：驗證 JAX-RS 健康檢查端點。
* `ListenerTest` (2 tests)：驗證 TIFF 與字型系統檢測 Listener。
* `ModelAndDtoTest` (8 tests)：驗證 Model/DTO 序列化與屬性。
* `PolicyServiceTest` (2 tests)：驗證業務層邏輯。
* `GlobalConfigTest` (1 test)：驗證全域設定讀取。
* `WebUtilsTest` (6 tests)：驗證 Web 工具類。

---

## 人工手動測試指南

若需在本機啟動 Tomcat 進行畫面、cURL、Postman 或 SoapUI 人工手動驗證，請參閱：
👉 **[系統全功能人工測試手冊 (MANUAL_TEST_GUIDE.md)](MANUAL_TEST_GUIDE.md)**
