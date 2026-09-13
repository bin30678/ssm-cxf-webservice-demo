# SSM + Apache CXF REST / SOAP 範例

傳統 WAR（非 Spring Boot），整合 Spring、Spring MVC、MyBatis、Apache CXF JAX-RS、JAX-WS、CXF Bus interceptor 與 Log4j2 個資隱碼。

## 技術版本

- Java 17+
- Spring 6.2
- MyBatis 3.5
- Apache CXF 4.1.8（Jakarta EE 10）
- Log4j2 2.26.1
- Tomcat 10.1+

## URL

CXFServlet 同時映射 `/rest/*` 與 `/ws/*`：

```text
GET  /ssm-cxf-webservice-demo/rest/api/health
GET  /ssm-cxf-webservice-demo/rest/api/policies/P-2026-0001
POST /ssm-cxf-webservice-demo/rest/api/policies/inquiry

SOAP /ssm-cxf-webservice-demo/ws/policy
WSDL /ssm-cxf-webservice-demo/ws/policy?wsdl
```

JAX-RS extension mapping 已在 `cxf-bus.xml` 的 `jaxrs:server` 中設定 `.json -> application/json`，因此也能呼叫：

```text
GET /ssm-cxf-webservice-demo/rest/api/policies/P-2026-0001.json
```

## REST 呼叫

```powershell
curl.exe "http://localhost:8080/ssm-cxf-webservice-demo/rest/api/health"

curl.exe "http://localhost:8080/ssm-cxf-webservice-demo/rest/api/policies/P-2026-0001"

curl.exe -X POST "http://localhost:8080/ssm-cxf-webservice-demo/rest/api/policies/inquiry" `
  -H "Content-Type: application/json" `
  -d '{"policyNo":"P-2026-0001","requesterName":"王小明","requesterPhone":"0912345678","requesterEmail":"wang@example.com","channel":"WEB"}'
```

有 body 的 REST request 僅接受 `application/json` 或 `application/*+json`。GET、HEAD、OPTIONS 沒有 body，因此不強制 Content-Type。

舊有 multipart upload 類別保留作參考，但未註冊至 JAX-RS server，因此不是公開 endpoint。

## SOAP 呼叫

`PolicyInquiryPort` 有兩個 operation：

- `getPolicyStatus`：回傳狀態字串。
- `getPolicySummary`：回傳 `PolicyInfo`，由 CXF JAXB data binding 自動轉成 SOAP XML。

SOAP 1.1：

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:pol="https://example.com/cxfdemo/policy">
  <soapenv:Header/>
  <soapenv:Body>
    <pol:getPolicySummary>
      <policyNo>P-2026-0001</policyNo>
    </pol:getPolicySummary>
  </soapenv:Body>
</soapenv:Envelope>
```

```powershell
curl.exe -X POST "http://localhost:8080/ssm-cxf-webservice-demo/ws/policy" `
  -H "Content-Type: text/xml; charset=UTF-8" `
  --data-binary "@soap-request.xml"
```

SOAP 1.1 僅接受 `text/xml`；SOAP 1.2 使用 `application/soap+xml`。

## CXF Bus

`cxf-bus.xml` 全域註冊：

- `RequestValidationInterceptor`：Content-Type 白名單、trace ID、StAX/XXE 安全限制。
- `ResponseHeaderInterceptor`：`X-Request-Id`、`X-Response-Time-Ms`、`nosniff` 與 UTF-8。
- `UnifiedFaultInterceptor`：將 SOAP 失敗轉成固定 `SOAP Fault/detail/ServiceError`。
- `LoggingFeature` + `PrivacyMaskingLogEventSender`：request、response、fault 在交給 Log4j2 前隱碼。

REST 錯誤由 `UnifiedExceptionMapper` 輸出固定格式：

```json
{
  "success": false,
  "code": "POLICY_NOT_FOUND",
  "message": "查無保單",
  "traceId": "...",
  "timestamp": "..."
}
```

XML 防護包含禁止 insecure parser，以及 element depth、child count、attribute count、text length、element count 與總字元數限制。

## 個資隱碼與 Log4j2

隱碼器會依 JSON/XML 欄位名稱、完整巢狀路徑與值格式判斷。預設規則包含：

- 密碼、PIN、token、secret、Authorization、Cookie、API key
- 要保人／被保人／受益人姓名
- 電話、Email、地址、身分證字號
- 信用卡號、CVV/CVC、銀行或登入帳號

一般欄位（例如商品名稱、保費、channel、status）會正常顯示。格式損壞或隱碼失敗時只記錄 `[PAYLOAD_REDACTED]`，不退回原文。

環境設定：

```powershell
# 測試環境，只能搭配假資料
$env:APP_ENVIRONMENT = "test"
$env:PRIVACY_MASKING_ENABLED = "false"

# 正式環境
$env:APP_ENVIRONMENT = "prod"
$env:PRIVACY_MASKING_ENABLED = "true"
```

若 `APP_ENVIRONMENT=prod` 且隱碼被關閉，Spring context 會拒絕啟動。正式環境也可用 `PRIVACY_PAYLOAD_LOGGING_ENABLED=false` 完全停用 payload logging。

Log4j2 輸出：

- Console：開發用途。
- Rolling JSON file：`${catalina.base}/logs/ssm-cxf-webservice-demo.log`。
- 每日或 10 MB 輪替，保留 30 天。
- CXF payload logging 上限預設 64 KB，不採無限制的 `limit=-1`。

## 建置與測試

```powershell
mvn clean test
mvn package
```

目前共有 39 個測試，涵蓋 Spring/CXF 啟動、UTF-8 中文隱碼關鍵字載入、JAX-RS resource、JAX-WS operation、MyBatis 實際讀寫、bus 組態、REST/SOAP Content-Type、XXE/DOCTYPE 拒絕與 StAX 安全屬性、response header、REST/SOAP fault、JSON/SOAP 個資與 principal 帳號隱碼、一般欄位保留、測試環境開關，以及正式環境禁止關閉隱碼。
