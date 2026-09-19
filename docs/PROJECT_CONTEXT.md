# 專案架構與修改影響索引

最後核對：2026-09-19。此文件供後續任務快速恢復脈絡；每次仍須核對程式、XML 與 Git 差異。

## 固定技術與入口

- 單一 Maven WAR；Java 8、Spring 3.2.14、CXF 3.0.6、MyBatis 3.3.0，使用 javax API。
- `web.xml` 建立 Spring 根容器 (`applicationContext.xml`)、Spring MVC 子容器 (`spring-mvc.xml`) 與 CXF Servlet。
- 同一 CXF Servlet 映射 `/rest/*` 與 `/Webservice/*`，共用 CXF Bus；這是路由前綴慣例，並非兩者隔離的存取控制。
- 以下路徑均需依部署方式加上 context path。

| 入口 | 流程與外部影響 |
| --- | --- |
| GET `/rest/health` | HealthResource；只回傳程式存活狀態，不測 DB 可用性 |
| GET `/rest/policies` | 目前僅 API 連線檢查訊息，不是保單清單 |
| POST `/rest/policies`、`/rest/policies/inquiry` | PolicyResourceImpl → PolicyService → DemoMapper → policy_info |
| POST `/rest/files/upload` | FileUploadResource → FileStorageService 寫本機檔案 → DemoMapper 新增 file_upload_record |
| `/Webservice/soap/policies?wsdl` | PolicyInquiryPortImpl → PolicyService，提供 getPolicyStatus/getPolicySummary |
| `/tasks/clean-backup`（及別名） | Spring MVC TaskController → ScheduledTasks，會刪除指定目錄過期檔案 |
| `/rest/policies/test-*` | 觸發非同步模擬、外部 JAR、外部 HTTP 或備份清理；不是純讀取操作 |

## 共用設定與攔截器

- `applicationContext.xml`：只暴露主庫 dataSource1，主庫 SqlSessionTemplate/JdbcTemplate/transactionManager1；掃描主專案與外部 JAR 的 Bean。
- `cxf-bus.xml`：Bus 共用 AuditRequest/AuditResponse/AuditFault；GsonProvider 和 UnifiedExceptionMapper 提供 REST JSON 與錯誤回應。
- RequestValidationInterceptor 明確接在 JAX-RS/JAX-WS 入站端點：REST 允許 JSON、+json、multipart/form-data；SOAP 允許 text/xml、application/soap+xml。各 REST 方法仍受 @Consumes 限制。
- ResponseHeaderInterceptor 接 CXF 正常回應與錯誤回應，含 X-Request-Id。
- UnifiedFaultInterceptor 只處理 SOAP Fault；REST 例外由 UnifiedExceptionMapper 處理。
- Spring MVC 只有原本的 AuditMvcInterceptor，不加入上述 CXF 三個攔截器。

## DB 與交易

- 主庫 JNDI `jdbc/cxfdemo1`。GenericDao 另提供 `jdbc/cxfdemo2`、`jdbc/as400_a`、`jdbc/as400_b`；本機 context.xml 均以 MySQL 模擬。
- MainDbOperationsService 支援 Mapper、SqlSessionTemplate、JdbcTemplate、原生 JDBC。混合交易依賴同一 DataSource 與 DataSourceUtils；修改時必跑提交／回滾測試。
- MainProjectIntegrationService 傳入同一 Connection 給 external-lib-a/b，管理 commit/rollback/close。
- 外部 JAR 也有自行 JNDI lookup 的方式，由 PolicyDaoEquivalenceTest 驗證獨立交易；不可籠統認為所有外部連線都由主專案傳入。

## 設定來源與寄信

- DatabasePropertyPlaceholderConfigurer：啟動時讀主庫 system_properties，提供 XML `${...}` 與 `@Value`。未載入 application.properties。
- FileStorageService 使用 `file.upload.dir` 與 `file.upload.max-size`（bytes）；變更 DB 值後需重新啟動應用使 @Value 生效。
- `src/main/resources/db/upload-settings.sql` 新增本機上傳目錄與 10485760 bytes（10 MiB）；重複執行保留已有值。腳本不會在啟動時自動執行。
- MailServiceImpl 是唯一寄信實作；sendMail(Mail) 內自行讀 ConfigSingleton 並建立 JavaMailSenderImpl，XML 沒有 mailSender Bean。
- ConfigSingleton 是獨立 DB 快取：首次非空載入後重用；DB 異動須 reload() 或重啟。此快取刷新不會刷新 FileStorageService 的 @Value。
- SMTP keys：mail.host、mail.port、mail.username、mail.password（可回退 mail.smtp.*）；mail.from（可回退 system.mail.from）；mail.transport.protocol、mail.smtp.auth、mail.smtp.starttls.enable、mail.debug。
- 寄信支援多收件人、CC、HTML、附件；缺少附件會警告並略過，寄信失敗會拋 RuntimeException。沒有真實 SMTP 寄送測試。

## 檔案傳輸的區別

- FileStorageService 使用 java.nio.file.Files：HTTP multipart → 應用伺服器本機磁碟；不呼叫 FtpService。
- FtpService 使用 Apache Commons Net FTPClient，登入、被動模式、binary storeFile：FTP。
- 未找到 TFTPClient、FTPSClient、SFTP/SSH 實作。公司實際協定需以公司端程式或設定確認。

## 修改影響與驗證

| 改動範圍 | 必須核對 |
| --- | --- |
| web.xml／cxf-bus.xml／Resource | REST、SOAP、Health、首頁連結、multipart、錯誤回應與 Spring MVC 隔離 |
| 入站驗證／Gson／回應 Header | Content-Type、X-Request-Id、SOAP Fault、REST JSON、檔案上傳 |
| DataSource／Mapper／DAO | 四種主庫讀寫、混合交易提交／回滾、四組 JNDI 與外部 JAR |
| Mail／ConfigSingleton | 不重新引入 XML mailSender、設定讀取、快取刷新、例外行為 |
| FileStorage／上傳設定 | DB 設定注入、實際檔案內容與 metadata、超限後無殘檔 |

執行 `mvn -o clean test`，再執行 `git diff --check`。依賴未備妥時先解決依賴／執行權限，不以舊 target 報告當成本次結果。

2026-09-19 本次驗證：`mvn -o clean test` 共 63 個案例、0 failures、0 errors、0 skipped。
本機 mysql8 容器的 cxfdemo1 已執行上傳設定 SQL 並查回兩個值；應用需重新啟動才會載入。
沙箱內 Maven 曾無法解析使用者的本機快取，但相同離線測試在取得執行權限後通過；不要直接判定是缺少 JAR。

- CxfServletIntegrationTest：正式 XML、真實 CXF/Dispatcher Servlet、模擬 Servlet request/response、H2 與暫存檔；涵蓋端點接線、JSON、SOAP 成功／錯誤、multipart、DB 上傳上限、MVC 隔離與郵件元件設定。不是外部 Tomcat HTTP socket 測試。
- PolicyDaoEquivalenceTest：主庫四種存取與交易、外部 JAR/JNDI 隔離。
- 其餘測試涵蓋保單、檔案、排程、Listener、設定、工具。RestTemplateUtilsTest 使用 MockRestServiceServer，不依賴外網。
- 未涵蓋：真實 SMTP／FTP／TFTP、正式 Oracle／AS400、真實 Tomcat 部署、所有故障與並發情境。測試通過不表示零回歸風險。

已知既有行為：上傳超限由 Resource 回 500；非同步銀行是隨機失敗模擬，既有測試只等待背景執行；privacy.* 目前無載入／遮罩實作；log4j2.xml 的 JsonTemplateLayout 需另外核對相依與啟動訊息。
