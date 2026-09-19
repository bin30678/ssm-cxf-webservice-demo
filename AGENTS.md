# 專案協作規則

- 使用繁體中文回覆。程式與技術問題先查證程式碼、設定及必要的官方文件；不知道的部分明確說明。
- 開始修改前，先讀 `docs/PROJECT_CONTEXT.md`，再確認 `git status` 及當次相關程式／XML。文件是索引，實際程式碼才是判斷依據。
- 先追蹤入口 → 服務 → DAO／外部資源與 Spring/CXF 接線，說明此次變動可能影響的其他功能。不可只看被點名的單一方法。
- 固定架構：Java 8、Spring 3.2 XML、CXF 3.0.6、javax API、WAR。未有新需求，不改成 Spring Boot/Jakarta 架構。
- 寄信只使用 `MailServiceImpl`，SMTP 設定由實作內讀 `ConfigSingleton`，不可重新加入 XML `mailSender`。
- CXF 驗證與回應攔截器只接 JAX-RS/JAX-WS 端點；Spring MVC 不接這三個攔截器。啟用驗證必須同時驗證 JSON、SOAP、multipart 上傳及錯誤回應。
- `system_properties` 是上傳設定來源；`application.properties` 目前不載入。HTTP 上傳存本機磁碟，`FtpService` 是另一條 FTP 流程，不可推論公司使用 TFTP/SFTP。
- 修改共用設定、攔截器、DAO 或服務註冊後，執行 `mvn -o clean test`（依賴已備妥時）及 `git diff --check`。clean 可清掉刪除 Java 類別後的殘留 class。
- `CxfServletIntegrationTest` 驗證正式 XML + Servlet + H2 的跨功能流程，`PolicyDaoEquivalenceTest` 驗證資料庫與交易。不可拿直接呼叫方法的測試取代接線驗證。
- 測試只使用 H2、暫存目錄與模擬 HTTP；不自動寄真實郵件、傳 FTP 或清理真實備份。真實資料庫修改依當次使用者授權執行。
- 若無法執行測試，清楚回報原因與未驗證範圍，不得宣稱全部正常。保留使用者已有的改動。
- 行為、路由、設定鍵或驗證方式改變時，同步更新 `docs/PROJECT_CONTEXT.md` 與相關使用文件，記錄驗證結果與尚未覆蓋部分。
