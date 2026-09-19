# 系統全功能人工測試手冊 (Manual Testing Guide)

本手冊提供系統所有客製商業邏輯、排程、外部整合與 Web 服務的**手動測試（人工驗證）完整步驟**。包含環境準備、觸發方式（URL / cURL / 畫面操作）、預期 Console 輸出與資料庫驗證 SQL。

---

## 目錄
1. [環境前置準備](#1-環境前置準備)
2. [功能一：系統啟動自動體檢（ServletListener / DB 設定載入 / Quartz）](#功能一系統啟動自動體檢servletlistener--db-設定載入--quartz)
3. [功能二：CXF REST API 與 Response 延遲緩衝寫入 DB (AuditLog)](#功能二cxf-rest-api-與-response-延遲緩衝寫入-db-auditlog)
4. [功能三：SOAP WebService 介面呼叫 (WSDL 與保單查詢)](#功能三soap-webservice-介面呼叫-wsdl-與保單查詢)
5. [功能四：非同步背景長輪詢呼叫外部銀行（20 次重試機制）](#功能四非同步背景長輪詢呼叫外部銀行20-次重試機制)
6. [功能五：外部 JAR 模組整合（Connection 傳遞與主專案事務控管）](#功能五外部-jar-模組整合connection-傳遞與主專案事務控管)
7. [功能六：Apache HttpClient 4.5 + RestTemplate（信任憑證與 API 日誌）](#功能六apache-httpclient-45--resttemplate信任憑證與-api-日誌)
8. [功能七：Spring @Scheduled 排程與凌晨 1 點備份檔案遞迴刪除](#功能七spring-scheduled-排程與凌晨-1-點備份檔案遞迴刪除)
9. [功能八：檔案上傳與路徑安全檢核 (FileUploadResource)](#功能八檔案上傳與路徑安全檢核-fileuploadresource)
10. [功能九：JavaMail 發信與 FTP 上傳手動整合指引](#功能九javamail-發信與-ftp-上傳手動整合指引)

---

## 1. 環境前置準備

### 步驟 1.1：資料庫初始化 (MySQL)
請確保本機已安裝 MySQL，並執行以下 SQL 腳本以建立雙資料庫（`cxfdemo1` 與 `cxfdemo2`）及所有測試資料表：
* **腳本路徑**：`ssm-cxf-webservice-demo/src/main/resources/` 或本機初始化腳本 `init_dbs_v3.sql`。
* **執行 SQL 重點摘要**：
```sql
-- 建立資料庫
CREATE DATABASE IF NOT EXISTS cxfdemo1 DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS cxfdemo2 DEFAULT CHARACTER SET utf8mb4;

-- cxfdemo1 表：audit_request, audit_response, audit_fault, external_api_log, system_properties, policy_info, transfer_task, transfer_task_detail
USE cxfdemo1;
CREATE TABLE IF NOT EXISTS system_properties (
    prop_key VARCHAR(100) PRIMARY KEY,
    prop_value VARCHAR(255) NOT NULL
);
INSERT INTO system_properties (prop_key, prop_value) VALUES 
('system.app.url', 'http://localhost:8080/cxfdemo')
ON DUPLICATE KEY UPDATE prop_value=VALUES(prop_value);

-- cxfdemo2 (AS400 模擬) 表：customers
USE cxfdemo2;
CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL
);
INSERT INTO customers (customer_name, status) VALUES 
('王大明', 'ACTIVE'), ('李小華', 'ACTIVE'), ('張三', 'INACTIVE');
```

### 步驟 1.2：啟動 Web 容器 (Tomcat)
在專案根目錄開啟命令提示字元或 PowerShell：
```bash
cd e:\antigravity_workspace\ssm-cxf-webservice-demo
mvn tomcat6:run
```
> **提示**：系統預設監聽在 `http://localhost:8080/`。

---

## 功能一：系統啟動自動體檢（ServletListener / DB 設定載入 / Quartz）

### 測試目的
驗證系統啟動時：
1. ServletContextListener 會主動檢查作業系統有無 **TIFF ImageReader** 與指定字型（預設：**標楷體**）。
2. `DatabasePropertyPlaceholderConfigurer` 在 Spring 啟動時透過 `beanFactory` 取得主庫 `DataSource` (`dataSource1`) 建立連線，從 DB1 撈取 `system_properties`，並由 `MethodInvokingFactoryBean` 注入靜態類別 `GlobalConfig`。
3. Quartz 排程在系統啟動 1 秒後自動觸發第一次執行，之後每 5 分鐘觸發一次。

### 測試步驟
1. 觀察執行 `mvn tomcat6:run` 時終端機輸出的 Console Log。

### 預期結果
Console 應出現以下關鍵 Log：
```text
=== [Listener Init] TIFF ImageReader is AVAILABLE in the system.
=== [Listener Init] Font '標楷體' is AVAILABLE in the system.
=== Loading Properties from Database using GenericDao ===
Loaded DB Property: system.app.url = http://localhost:8080/cxfdemo
=== [GlobalConfig] url is set to: http://localhost:8080/cxfdemo ===
...
=== [Quartz Job] Triggered at: Sat Sep 12 10:30:01 CST 2026 ===
```

---

## 功能二：CXF REST API 與 Response 延遲緩衝寫入 DB (AuditLog)

### 測試目的
驗證 CXF 攔截器架構：
- 進入時 `AuditRequestInterceptor` 記錄 Request 資訊。
- 回應時透過 `CacheAndWriteOutputStream` 與 Callback，**在 Stream close (`onClose`) 階段**才完整抓取 Payload，並透過 `AuditLogDao`（使用 `SqlSessionTemplate`）寫入 DB。

### 測試步驟
在終端機執行 cURL 或開啟瀏覽器：
```bash
curl -X GET http://localhost:8080/rest/policies
```

### 預期結果
1. **HTTP 回應**：
   ```json
   {"status":"ok", "message":"CXF REST API is working!"}
   ```
2. **Console Log 輸出**：
   ```text
   === [Audit Request] URI: /rest/policies, Method: GET, IP: 127.0.0.1 ===
   === [Audit Response (onClose)] Response Stream Closed. Payload size: 53 bytes ===
   ```
3. **資料庫驗證**：
   在 MySQL `cxfdemo1` 執行查詢：
   ```sql
   SELECT * FROM audit_request ORDER BY id DESC LIMIT 1;
   SELECT * FROM audit_response ORDER BY id DESC LIMIT 1;
   ```
   確認有最新一筆請求 URI 與對應的 Response Code (200)。

---

## 功能三：SOAP WebService 介面呼叫 (WSDL 與保單查詢)

### 測試目的
驗證 JAX-WS SOAP 服務能正常產出 WSDL 並支援 SOAP 訊息查詢。

### 測試步驟

#### 3.1 驗證 WSDL
開啟瀏覽器訪問：
```
http://localhost:8080/Webservice/soap/policies?wsdl
```
**預期**：頁面能完整顯示 XML 格式的 WSDL 定義檔，包含 `PolicyInquiryService` 與相關 operation。

#### 3.2 發送 SOAP 請求
使用 Postman 或 SoapUI 發送 POST 請求至 `http://localhost:8080/Webservice/soap/policies`：
* **Header**：`Content-Type: text/xml; charset=utf-8`
* **Body**：
  ```xml
  <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:pol="https://example.com/cxfdemo/policy">
     <soapenv:Header/>
     <soapenv:Body>
        <pol:getPolicyStatus>
           <arg0>POL123456</arg0>
        </pol:getPolicyStatus>
     </soapenv:Body>
  </soapenv:Envelope>
  ```
**預期結果**：回傳 200 OK，Body 包含 `<return>ACTIVE</return>`（若有建立假資料）或 `<return>NOT_FOUND</return>`。

---

## 功能四：非同步背景長輪詢呼叫外部銀行（20 次重試機制）

### 測試目的
模擬呼叫 API 後啟動 `new Thread` 執行長任務：
- 最多跑 20 次迴圈，每次嘗試四個步驟（1.要資料 -> 2.處理JSON -> 3.轉銀行格式 -> 4.傳給銀行）。
- 成功時更新主表狀態，失敗時記錄子表並重試。

### 測試步驟
發送 GET 請求觸發測試：
```bash
curl -X GET http://localhost:8080/rest/policies/test-async-bank
```

### 預期結果
1. **HTTP 回應（立即返回，不阻塞）**：
   ```json
   {"message":"Async bank transfer started in background. Check console!"}
   ```
2. **Console 觀察背景執行緒日誌**：
   ```text
   主表已建立，Task ID: d4c99993-..., 開始執行...
   --- 開始第 1 回 ---
   [DB] 主表更新: 1
   [DB] 寫入子表 -> 步: 1, 成功: true, 訊息: 步驟1成功: 取得資料
   [DB] 主表更新: 2
   [DB] 寫入子表 -> 步: 2, 成功: true, 訊息: 步驟2成功: 處理 JSON
   ...
   Task d4c99993-... 處理成功，結束！
   ```
   *(註：如果隨機觸發逾時失敗，會看到記錄子表失敗訊息，等待重試。)*

---

## 功能五：外部 JAR 模組整合（Connection 傳遞與主專案事務控管）

### 測試目的
驗證系統引用外部兩個獨立 JAR 模組：
- **`external-lib-a`**：Service + DAO 架構。
- **`external-lib-b`**：純 DAO 架構。
- 兩者的方法皆**要求傳入 `Connection`**，其連線開啟、Commit、Rollback 與**最終 Close 全部由主專案控管**。

### 測試步驟
發送 GET 請求觸發外部模組整合服務：
```bash
curl -X GET http://localhost:8080/rest/policies/test-external-jar
```

### 預期結果
1. **HTTP 回應**：
   ```json
   {"message":"External JAR simulated. Check console for DB2 connection passing!"}
   ```
2. **Console 輸出**：
   ```text
   === [External JAR A] getActiveCustomers called ===
   Active customers from Lib A (Service+DAO): [王大明, 李小華]
   === [External JAR B] getInactiveCustomerCount called ===
   Inactive customer count from Lib B (DAO only): 1
   === Connection completely closed by Main Project ===
   ```
   **驗證核心**：確認看到 `=== Connection completely closed by Main Project ===`，代表外部 JAR 均未擅自關閉連線，連線生命週期完全由主專案掌控。

---

## 功能六：Apache HttpClient 4.5 + RestTemplate（信任憑證與 API 日誌）

### 測試目的
驗證 `RestTemplateUtils`：
- 自訂 Apache HttpClient 4.5 繞過並信任所有 SSL 憑證。
- 發送外網請求後，無論成功或拋出例外，`finally` 區塊必將【URL、參數、返回值、例外訊息、時間】透過 `ExternalApiLogDao` 寫入 `external_api_log` 資料表。

### 測試步驟
執行呼叫端點：
```bash
curl -X GET http://localhost:8080/rest/policies/test-rest-template
```

### 預期結果
1. **HTTP 回應**：
   ```json
   {"message":"External API called and logged to DB!", "api_response_length": 308}
   ```
2. **資料庫驗證**：
   在 MySQL `cxfdemo1` 查詢：
   ```sql
   SELECT id, url, response_body, exception_msg, call_time FROM external_api_log ORDER BY id DESC LIMIT 1;
   ```
   確認 `url` 為 `https://httpbin.org/get`，且 `response_body` 存有真實回傳的 JSON 字串。

---

## 功能七：Spring @Scheduled 排程與凌晨 1 點備份檔案遞迴刪除

### 測試目的
驗證 `@Scheduled` 設定的 4 組排程：
- 每小時整點：`0 0 * * * *`
- 每天凌晨零時：`0 0 0 * * *`
- 每天中午十二點：`0 0 12 * * *`
- **每天凌晨一點 (`0 0 1 * * *`)**：遞迴掃描備份目錄，依檔案「最後修改時間 (`lastModified`)」自動刪除超過 7 天的過期備份檔與空資料夾。

### 測試步驟（手動打網址立即觸發）
本系統已提供 **REST API 控制器**，無須等待定時排程，可直接在瀏覽器或以 HTTP GET 打網址立即觸發清理：

#### 觸發方式 A：Spring MVC 控制器 (`TaskController`)
在瀏覽器網址列或終端機輸入：
```text
# 1. 使用預設值 (清理 C:/backup_folder，保留 7 天)
http://localhost:8080/tasks/clean-backup

# 2. 自訂目標目錄與保留天數 (例如保留 3 天)
http://localhost:8080/tasks/clean-backup?dir=C:/backup_folder&days=3
```

#### 觸發方式 B：CXF JAX-RS REST 端點 (`PolicyResource`)
```text
http://localhost:8080/rest/policies/test-clean-backup?dir=C:/backup_folder&days=7
```

### 回傳 JSON 範例
```json
{
  "success": true,
  "message": "過期檔案清理完成",
  "targetDirectory": "C:\\backup_folder",
  "daysToKeep": 7,
  "deletedFilesCount": 1,
  "deletedFiles": [
    "C:\\backup_folder\\old_file.txt"
  ],
  "deletedDirectoriesCount": 0,
  "deletedDirectories": []
}
```

### 預期結果
1. 瀏覽器或工具收到上述 JSON 回應，顯示成功刪除的過期檔案數量與清單。
2. Console 輸出：
```text
Deleted old backup file: C:\backup_folder\old_file.txt
Deleted empty directory: C:\backup_folder\old_sub_dir
```
3. 檔案系統中 `old_file.txt` 確實被清除，未滿指定天數之檔案（如 `new_file.txt`）完好保留。

---

## 功能八：檔案上傳與路徑安全檢核 (FileUploadResource)

這是 HTTP multipart 上傳到應用伺服器本機磁碟，不是 FTP/TFTP。
在主庫 `cxfdemo1` 執行 `src/main/resources/db/upload-settings.sql`，新增 `file.upload.dir` 與
`file.upload.max-size`（預設建議 10485760 bytes / 10 MiB）。已有設定不覆蓋，其他部署須先調整目錄。
DB 設定變更後重新啟動應用；`application.properties` 目前未載入。

### 測試目的
驗證檔案上傳服務：
- 過濾檔名非法字元。
- 防止目錄遍歷路徑攻擊（Path Traversal）。
- 限制上傳最大檔案大小。

### 測試步驟
使用 cURL 上傳檔案：
```bash
curl -X POST http://localhost:8080/rest/files/upload \
  -H "Content-Type: multipart/form-data" \
  -F "file=@pom.xml;type=text/xml" \
  -F "description=Maven POM 檔案"
```

### 預期結果
回傳 HTTP 200 與上傳結果 JSON：
```json
{
  "status": "SUCCESS",
  "id": 1,
  "storedName": "xxxx-xxxx-pom.xml"
}
```

---

## 功能九：JavaMail 發信與 FTP 上傳手動整合指引

因本機環境通常未架設公開的 SMTP 與 FTP Server，若需手動驗證此兩項服務，請依以下設定進行：

### 9.1 JavaMail 發信測試
1. 在主庫 `system_properties` 設定實際 SMTP 的 `mail.host`、`mail.port`、`mail.username`、`mail.password`、`mail.from`，以及 `mail.smtp.auth`、`mail.smtp.starttls.enable`。
2. `MailServiceImpl` 內讀取 `ConfigSingleton` 並自行建立寄信元件；Spring XML 不設定 `mailSender`。DB 異動後呼叫 `ConfigSingleton.getInstance().reload()` 或重啟。
3. 在 Controller/Service 中注入 `MailService`，呼叫：
   ```java
   Mail mail = new Mail("測試信件", "<p>這是內文</p>");
   mail.addTo("receiver@test.com");
   mail.addCc("cc@test.com");
   mailService.sendMail(mail);
   ```
4. 觀察收件人信箱是否收到郵件。

### 9.2 FTP 上傳測試
1. 在測試環境或本機架設 FileZilla Server。
2. 注入 `FtpService`，呼叫 `uploadFile(host, port, username, password, remoteDir, localFile)`，其中 localFile 是 `java.io.File`。
3. 此類別使用 `FTPClient`，目前沒有 TFTP／SFTP／FTPS 實作。公司實際協定需另外確認。

---

## 快速驗證 Checklist 彙整表

| 編號 | 功能描述 | 快速驗證途徑 | 預期確認點 |
|:---:|:---|:---|:---|
| 1 | TIFF / 字型檢查 ServletListener | 啟動 Tomcat | 觀察 Console 是否出現 `[Listener Init]` 判定資訊 |
| 2 | DB 參數注入靜態類別 | 啟動 Tomcat | 觀察 `Loaded DB Property` 及 `GlobalConfig.getUrl()` 是否有值 |
| 3 | Quartz 排程觸發 | 啟動 Tomcat 等待 1 秒 | Console 印出 `[Quartz Job] Triggered at...` |
| 4 | CXF Response 緩衝寫入 DB | `GET /rest/policies` | Console 出現 `[Audit Response (onClose)]`，DB 查出 audit 紀錄 |
| 5 | SOAP WebService 呼叫 | 瀏覽器開 `/Webservice/soap/policies?wsdl` | 畫面正常載入 WSDL XML |
| 6 | 非同步 20 次銀行長輪詢 | `GET /rest/policies/test-async-bank` | 立即回傳 200，Console 背景印出步驟 1~4 重試過程 |
| 7 | 外部 JAR Connection 控管 | `GET /rest/policies/test-external-jar` | Console 印出 Lib-A、Lib-B 執行紀錄，最後印出主專案關閉連線 |
| 8 | HttpClient 繞過 SSL 寫 Log | `GET /rest/policies/test-rest-template`| 外網請求成功，MySQL `external_api_log` 新增 1 筆紀錄 |
