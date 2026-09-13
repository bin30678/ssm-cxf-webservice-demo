# 保留 JAX-RS：拆成 Spring Boot 4.1.1／CXF 4.2.x 服務的設定說明

查證日期：2026-09-13。

本文依目前模擬專案與你描述的公司實況整理：入口以 JAX-RS 為主，業務按機構／功能分组；外部 JAR 接收 JDBC Connection 或 AS400 物件。範例保留 JAX-RS。程式區塊是目標配置的示例，須套入實際類別與相依套件；這次未建立、編譯或啟動新的服務，也未更改既有程式。

## 1. 先回答共用 JAR 能不能交給 Spring Boot 管理

**可以。普通 JAR 也可以包含 Service、DAO、interceptor、provider、configuration。**

依賴與 Bean 註冊是不同的工作：

| 動作 | 解決的問題 |
|---|---|
| Maven dependency | 讓應用程式可以載入 JAR 裡的 class／資源 |
| component scan | 找出指定 package 中帶 Spring 元件註解的類別，建立 Bean |
| @Import＋@Bean | 明確匯入設定，指定哪些物件交給 Spring 建立、注入與管理 |
| Boot auto-configuration | 依 JAR 內的標準入口與條件，自動載入設定 |
| CXF endpoint／Bus 註冊 | 決定 Resource、provider、interceptor 何時參與 HTTP 請求處理 |

因此，「加入 dependency → 可引用類別 → 註冊成 Spring Bean → 掛入 CXF」是不同層次。純 Service 通常不需要最後一步；interceptor／Resource 則需要框架註冊。

掃描以 Java package 為界，並不以 JAR 為界。啟動類預設掃描自身 package 及子 package；外部 JAR 的 class 若在該範圍、具有可辨識的元件註解，也可以被掃到。普通 JAR 可以用明確的 @Import 避免放大掃描範圍。[Spring 官方共用 library 範例](https://spring.io/guides/gs/multi-module/)、[Boot Bean 與依賴注入](https://docs.spring.io/spring-boot/reference/using/spring-beans-and-dependency-injection.html)。

同一共用 JAR 被銀行服務、公會服務引用，**每個服務的 ApplicationContext 都建立自己的一組 Bean**。程式碼共用，但連線池、記憶體、靜態變數與執行緒不會因此跨程序共用。

## 2. 共用 JAR、自訂 starter、獨立服務怎麼選

| 形式 | 使用方式 | 適用情況 |
|---|---|---|
| 普通工具 JAR | dependency 後呼叫靜態方法／自行建立無相依物件 | 字串、格式、計算等純工具 |
| 普通 Spring 共用 JAR | dependency＋@Import，共用 JAR 內用 @Bean 或受限的 scan | 現階段共用 interceptor、provider、整合工具 |
| 自訂 starter | dependency 帶入 auto-configuration，依條件建立預設 Bean | 多個服務的配置已穩定，想統一啟用方式與可覆寫規則 |
| 獨立部署的服務 | 以 HTTP／訊息等契約呼叫 | 需要獨立的業務責任、部署與資料管理 |

**建議先用「普通共用 JAR＋@Import」，讓第一個服務的初始化清楚可見；共同配置穩定後，再封裝成 starter。** 不必為了用了 Spring Boot，立即把每種共用類別都做成 starter，或各自部署成一個服務。

Starter 本身也是 Maven JAR／dependency。它通常組合「需要的套件」與「自動設定」，不是另一種執行容器。可以分 core、autoconfigure、starter 三個 module，也可以先合併，沒有一定要三個 module 的要求。[Boot 自訂 auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)。

業務邊界方面，你公司的機構分組可作為候選。依 Zhamak Dehghani 的拆分經驗，切分時要連同業務能力、資料相依考量，而非僅按程式層次分組。因此本文建議將被選中的 API 及其業務 Service／DAO／VO 一起帶進新服務，讓共用 JAR 主要承載真正共通的整合機制。[作者原文](https://martinfowler.com/articles/break-monolith-into-microservices.html)。

## 3. 各種類別究竟要不要設定

| 元件 | 只靠一般 Spring scan 夠嗎 | 還需要什麼 |
|---|---|---|
| Service 實作類 | 有 @Service 且在掃描範圍，通常夠 | 所依賴的 DAO／client 等必須也有 Bean；多實作時選定要注入哪個 |
| Service 介面 | 不夠 | 需要具體實作 Bean，介面本身不會自動生出實作 |
| DAO 實作類 | 有 @Repository 且在掃描範圍，可成為 Bean | 所使用的 SqlSessionTemplate／JdbcTemplate／DataSource 要先配置 |
| 一般 DAO 介面＋impl | 掃描 impl 可以 | 不要把一般 DAO 介面誤交给 MyBatis 產生代理 |
| MyBatis Mapper 介面 | 一般 component scan 不夠 | @MapperScan，或符合 MyBatis starter 的 @Mapper 自動掃描條件 |
| 使用 SqlSessionTemplate 的 DAO | DAO 可以掃描 | 需要對應 SqlSessionFactory、Template、Mapper SQL 資源 |
| 使用 JdbcTemplate 的 DAO | DAO 可以掃描 | 需要對應 DataSource／JdbcTemplate |
| 接收 Connection 的外部 JAR | 視其類別有無註解 | 可 scan、@Import 或 @Bean；連線與交易仍要有明確管理者 |
| CXF interceptor | 可以成為 Bean | 還要加入 endpoint 或 Bus 的 interceptor list |
| JAX-RS Resource | @Component／@Service 可使其成為 Bean | JAX-RS @Path 本身不等於 Spring 元件註冊；還要發布到 CXF Server |
| JAX-RS provider／ExceptionMapper | 可以成為 Bean | 還要加入該 CXF Server 的 providers |
| Servlet Filter | 可以成為 Bean | 明確路徑、順序、init-param 用 FilterRegistrationBean |
| 純 static Utils | 不需要 | dependency 即可；沒有依賴注入、資源生命週期就不必硬做 Bean |
| 有設定或依賴的工具 | 可以 @Component，或 @Bean | 注入設定／client，必要時設定關閉方法 |
| VO／DTO | 通常不需要 | 作為資料物件建立、序列化；MyBatis type alias 與 Bean scan 是兩件事 |
| @ConfigurationProperties 類別 | 註解本身不保證註冊 | @EnableConfigurationProperties、@ConfigurationPropertiesScan，或 @Bean 綁定 |
| @Scheduled 工作 | Bean 註冊之外還需要啟用 | @EnableScheduling；並決定哪些部署實例要執行 |
| @Async 工作 | Bean 註冊之外還需要啟用 | @EnableAsync、合適的 executor；要透過代理呼叫 |
| Quartz Job | 不是加 @Component 就完成排程 | 配置 JobDetail／Trigger，交由 Quartz scheduler 執行 |

MyBatis 明確區分一般 component scan 與 Mapper 掃描；Mapper 需要建立代理。[MyBatis Mapper 註冊](https://mybatis.org/spring/mappers.html)。下面將重要類型逐一示範。

## 4. 選定幾支 API 後，新服務的目錄可如何安排

下面名稱只是示例。銀行服務可以收納數個業務上適合一起部署的機構，沒有強制一個機構一個程序。

~~~text
bank-api-service                       ← 可執行的 Spring Boot 應用
  com.company.bankapi
    BankApiApplication
    config
      JaxrsConfiguration
      MapperConfiguration
      AuditStorageConfiguration
    bank
      resource
      service
        impl
        dao
          impl
        vo
    anotherbank
      resource
      service ...
  resources
    application.yaml
    mapper/bank/*.xml

company-integration-common             ← 普通 library JAR
  com.company.shared
    config/CompanyCommonConfiguration
    interceptor
    provider
    utils
    audit                             ← 若確定共用稽核寫入實作

external-lib-a                         ← 既有外部 JAR 升級版
external-lib-b                         ← 既有外部 JAR 升級版
~~~ 

啟動類放在本服務根 package：

~~~java
package com.company.bankapi;

import com.company.shared.config.CompanyCommonConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(CompanyCommonConfiguration.class)
public class BankApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankApiApplication.class, args);
    }
}
~~~

本服務 com.company.bankapi 底下的設定與元件被預設掃描；com.company.shared 的指定設定由 @Import 匯入。**不要為了載入共用 JAR，就掃描整個 com.company，順帶把其他服務的 Resource、排程與配置一起載入。**

另一種可行方式是設定 scanBasePackages 或 scanBasePackageClasses，讓掃描涵蓋特定共用套件；但同一組類別應選定一種註冊策略，避免既 scan 又 @Bean 建立重複實例。共用 JAR 不需要 main，也不應用 Boot repackage 把它做成一般 dependency 難以引用的可執行封裝。

## 5. 服務的 Maven 與啟動設定

以下是 POM 片段，不是完整 POM：

~~~xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
    <relativePath/>
</parent>

<properties>
    <java.version>21</java.version>
    <cxf.version>4.2.3</cxf.version>
    <mybatis-starter.version>4.0.1</mybatis-starter.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.apache.cxf</groupId>
        <artifactId>cxf-spring-boot-starter-jaxrs</artifactId>
        <version>${cxf.version}</version>
    </dependency>
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>${mybatis-starter.version}</version>
    </dependency>
    <dependency>
        <groupId>com.company</groupId>
        <artifactId>company-integration-common</artifactId>
        <version>1.0.0</version>
    </dependency>
    <!-- 再加入實際資料庫 driver、升級後 external-lib-a / external-lib-b -->
</dependencies>
~~~

Java 21 是這份示例的選擇；Boot 4.1.1 最低要求 Java 17，並要求 Framework 7.0.9 以上。[Boot 系統需求](https://docs.spring.io/spring-boot/system-requirements.html)。

CXF 4.2.3 是本次查到的明確 4.2.x 版本。[CXF 下載頁](https://cxf.apache.org/download.html)、[CXF 4.2.3 starter POM](https://github.com/apache/cxf/blob/cxf-4.2.3/integration/spring-boot/starter-jaxrs/pom.xml)。MyBatis starter 4.0 系列對應 Boot 4；示例使用已发布的 4.0.1，不代表宣稱其為最新版本。[MyBatis 官方 repository](https://github.com/mybatis/spring-boot-starter)、[相容性矩陣](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)。

以 Boot parent／BOM 管理 Spring 版本，再明確管理 CXF、MyBatis starter、公司 JAR 版本。不要把舊的 Spring 6 或 mybatis-spring 3.0.3 強制覆寫進這套 Boot 4 示例。外部 JAR 是否相容還需檢查其依賴與 API；JAR 能註冊成 Bean 不等於二進位相容性已驗證。

共用 library 若使用公司 parent，應管理相容版本，僅引入自己實際需要的 Spring／CXF／MyBatis API。普通 common JAR 不必反過來依賴整個應用 starter；可執行服務才使用 spring-boot-maven-plugin 的 repackage。

## 6. Service：什麼情況只需 scan

假設公司保留 service／impl：

~~~java
// com.company.bankapi.bank.service.PolicyService
public interface PolicyService {
    PolicyVo find(String policyNo);
}

// com.company.bankapi.bank.service.impl.PolicyServiceImpl
@Service
public class PolicyServiceImpl implements PolicyService {
    private final PolicyDao policyDao;

    public PolicyServiceImpl(PolicyDao policyDao) {
        this.policyDao = policyDao;
    }

    @Override
    public PolicyVo find(String policyNo) {
        return policyDao.find(policyNo);
    }
}
~~~

以上省略各類別的 package／import；PolicyVo 沿用該服務的資料物件，@Service 來自 org.springframework.stereotype。

此類別在啟動類的子 package、有 @Service，且 PolicyDao 已註冊，通常不需要再寫一個 ServiceConfiguration。單一建構子也不需要額外加 @Autowired。[Boot 依賴注入](https://docs.spring.io/spring-boot/reference/using/spring-beans-and-dependency-injection.html)。

幾個界線要清楚：

- 舊 Service 若原本只有 XML bean 定義，類別沒有註解，搬進新服務不會憑名稱自動變 Bean；補 @Service 或用 @Bean。
- 原本 XML 有 property／constructor-arg 值，仍需改成建構子依賴或配置綁定。
- 有兩個同介面的實作時，用不同介面分責任，或用 @Qualifier／@Primary 明確選擇。
- 透過 Spring 注入 Service 才會取得其代理。自行 new 或 this 呼叫會影響 @Transactional／@Async 的代理行為。[Spring 交易代理規則](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)。

如果想維持無 Spring 註解的業務實作，也可以在本服務設定中建立：

~~~java
@Configuration(proxyBeanMethods = false)
public class BusinessConfiguration {
    @Bean
    PolicyService policyService(PolicyDao policyDao) {
        return new PolicyServiceImpl(policyDao);
    }
}
~~~

這是上一個 @Service 方式的替代方案，同一實作擇一使用。

## 7. DAO：保留原本 DAO／impl 寫法也可行

### 7.1 一般 DAO 介面＋自己實作

~~~java
public interface PolicyDao {
    PolicyVo find(String policyNo);
}

@Repository
public class PolicyDaoImpl implements PolicyDao {
    private final SqlSessionTemplate sqlSession;

    public PolicyDaoImpl(SqlSessionTemplate sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Override
    public PolicyVo find(String policyNo) {
        return sqlSession.selectOne(
            "bank.PolicyStatements.find", policyNo);
    }
}
~~~

@Repository 來自 org.springframework.stereotype；SqlSessionTemplate 來自 org.mybatis.spring。這個例子不需要 @MapperScan 掃描 PolicyDao。它是自己寫實作的普通介面，SQL 由 namespace＋statement id 查找。需要把包含 bank.PolicyStatements.find 的 Mapper XML 載入對應 SqlSessionFactory。

若 DAO 原本使用 JdbcTemplate，保留 @Repository，改為注入 JdbcTemplate 即可；多資料來源時用 @Qualifier 指定哪一個。SqlSessionTemplate 可以由多個 DAO 共用，並依 Spring 交易管理其 SqlSession。[MyBatis SqlSessionTemplate](https://mybatis.org/spring/sqlsession.html)。

### 7.2 MyBatis Mapper 介面，由框架產生實作

這是另一種 DAO 寫法，與上一節擇需要採用：

~~~java
package com.company.bankapi.bank.service.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PolicyMapper {
    PolicyVo find(@Param("policyNo") String policyNo);
}
~~~

~~~java
package com.company.bankapi.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@MapperScan(
    basePackages = "com.company.bankapi.bank.service.dao",
    annotationClass = Mapper.class
)
public class MapperConfiguration {
}
~~~

刻意加 annotationClass＝Mapper.class，是因你公司的 dao 目錄可能同時有一般介面及 MyBatis Mapper；只把有 @Mapper 的介面交給 MyBatis。一般 DAO 介面不要加 @Mapper。

這裡是單一 SqlSessionFactory／Template 的設定；多資料來源要再指定 sqlSessionTemplateRef，見第 11 節。[MyBatis Mapper 掃描](https://mybatis.org/spring/mappers.html)。

### 7.3 單一資料來源，哪些 Bean 可以交 starter 自動建立

在有 JDBC driver、MyBatis starter，且沒有自行提供同類配置的典型單資料來源情況：

~~~yaml
spring:
  datasource:
    url: ${BANK_DB_URL}
    username: ${BANK_DB_USER}
    password: ${BANK_DB_PASSWORD}

mybatis:
  mapper-locations:
    - classpath*:mapper/bank/**/*.xml
    - classpath*:mapper/company-audit/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
~~~

Boot 配置 DataSource 等 JDBC 基礎設施；MyBatis starter 根據該 DataSource 建立 SqlSessionFactory、SqlSessionTemplate。必要時再加入交易註解，而不是每個 Service 都建立一條連線。[MyBatis starter 設定](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)。

**MyBatis Mapper XML 可以保留。** 這裡改寫的是 Spring Bean 裝配 XML，不需要把 SQL 全部改成 Java annotation。放在共用 JAR 的 mapper XML 也可以用 classpath* 載入；使用分明路徑／namespace，避免把其他業務的 SQL 一起載進來。[SqlSessionFactoryBean 與 mapperLocations](https://mybatis.org/spring/factorybean.html)。

如果手動建立 SqlSessionFactory，mybatis.* 屬性不會自動套到你建立的每一個 factory；要明確設定或自行接入相應 customizer。

## 8. 普通共用 JAR：Configuration 怎麼寫

下面對應模擬專案的稽核與 provider。示例假設移入 common 的類別已升級為 Jakarta 版本，並把 AuditLogDao 相依改成建構子注入：

~~~text
AuditRequestInterceptor(AuditLogDao dao)
AuditResponseInterceptor(AuditLogDao dao)
AuditFaultInterceptor(AuditLogDao dao)
AuditLogDao(SqlSessionTemplate sqlSessionTemplate)
~~~

以上是建議新增的目標建構子，**原始模擬類別目前並沒有這些建構子**。其業務內容可沿用；既有 jakarta.annotation.Resource 注入也可以繼續使用，不是規定必須全改建構子。

共用 JAR 內：

~~~java
package com.company.shared.config;

import com.company.shared.audit.AuditLogDao;
import com.company.shared.interceptor.AuditRequestInterceptor;
import com.company.shared.interceptor.AuditResponseInterceptor;
import com.company.shared.interceptor.AuditFaultInterceptor;
import com.company.shared.provider.GsonProvider;
import com.company.shared.provider.UnifiedExceptionMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CompanyCommonConfiguration {
    @Bean
    public AuditRequestInterceptor auditRequestInterceptor(AuditLogDao dao) {
        return new AuditRequestInterceptor(dao);
    }

    @Bean
    public AuditResponseInterceptor auditResponseInterceptor(AuditLogDao dao) {
        return new AuditResponseInterceptor(dao);
    }

    @Bean
    public AuditFaultInterceptor auditFaultInterceptor(AuditLogDao dao) {
        return new AuditFaultInterceptor(dao);
    }

    @Bean
    public GsonProvider gsonProvider() {
        return new GsonProvider();
    }

    @Bean
    public UnifiedExceptionMapper unifiedExceptionMapper() {
        return new UnifiedExceptionMapper();
    }
}
~~~

@Bean 方法回傳的物件會交給 Spring；方法參數由 Spring 注入。因此它可以有依賴、初始化與關閉生命週期，並不侷限於 static Utils。這個 configuration 透過第 4 節 @Import 載入。[Spring Java configuration 的組合方式](https://docs.spring.io/spring-framework/reference/core/beans/java/composing-configuration-classes.html)。

本服務再決定稽核 DAO 使用的資料來源。單資料來源例子：

~~~java
package com.company.bankapi.config;

import com.company.shared.audit.AuditLogDao;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AuditStorageConfiguration {
    @Bean
    public AuditLogDao auditLogDao(SqlSessionTemplate template) {
        return new AuditLogDao(template);
    }
}
~~~

這樣共用的是稽核 DAO 程式，各服務仍明確決定使用哪個資料庫。這只是沿用共用稽核 DB 的過渡示例；稽核失敗是否影響 API、是否需獨立交易、應記哪些欄位，要保留公司實際要求。

共用 JAR 的依赖需要包含以上類別真正使用的套件；GsonProvider 也要有相容的 Gson dependency。沒有用到稽核寫 DB 的服務，可以拆出不依賴 AuditLogDao 的配置組合，不必被迫引入資料庫功能。

## 9. JAX-RS 與 CXF interceptor：Bean 註冊完還要掛到哪裡

### 9.1 JAX-RS Resource 保留原方式

可保留 JAX-RS 介面與實作。下面用單一具體類別縮短示例：

~~~java
package com.company.bankapi.bank.resource;

import com.company.bankapi.bank.service.PolicyService;
import com.company.bankapi.bank.service.vo.PolicyVo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;

@Component
@Path("/policies")
@Produces(MediaType.APPLICATION_JSON)
public class BankPolicyResource {
    private final PolicyService policyService;

    public BankPolicyResource(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GET
    @Path("/{policyNo}")
    public PolicyVo find(@PathParam("policyNo") String policyNo) {
        return policyService.find(policyNo);
    }
}
~~~

這只是連接方式示例；真實 API 的 HTTP status、查無資料處理、輸出欄位與例外格式仍沿用原契約。

### 9.2 明確建立 CXF Server，列出本服務要發布的 Resource

~~~java
package com.company.bankapi.config;

import java.util.List;
import com.company.bankapi.bank.resource.BankPolicyResource;
import com.company.shared.interceptor.AuditRequestInterceptor;
import com.company.shared.interceptor.AuditResponseInterceptor;
import com.company.shared.interceptor.AuditFaultInterceptor;
import com.company.shared.provider.GsonProvider;
import com.company.shared.provider.UnifiedExceptionMapper;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JaxrsConfiguration {
    @Bean(destroyMethod = "destroy")
    public Server bankApiServer(
            @Qualifier(Bus.DEFAULT_BUS_ID) Bus bus,
            BankPolicyResource policyResource,
            GsonProvider gsonProvider,
            UnifiedExceptionMapper exceptionMapper,
            AuditRequestInterceptor auditIn,
            AuditResponseInterceptor auditOut,
            AuditFaultInterceptor auditFault) {

        var endpoint = new JAXRSServerFactoryBean();
        endpoint.setBus(bus);
        endpoint.setAddress("/");

        // 加入這次選定的 Resource；其他 API 以逗號加入同一 List。
        endpoint.setServiceBeans(List.of(policyResource));
        endpoint.setProviders(List.of(gsonProvider, exceptionMapper));

        endpoint.getInInterceptors().add(auditIn);
        endpoint.getOutInterceptors().add(auditOut);
        endpoint.getInFaultInterceptors().add(auditFault);
        endpoint.getOutFaultInterceptors().add(auditFault);

        return endpoint.create();
    }
}
~~~

~~~yaml
cxf:
  path: /rest
  jaxrs:
    component-scan: false
    classes-scan: false
~~~

此例在未另設 context path 時，URL 為：

~~~text
/rest                  ← CXFServlet mapping 前綴
     /                 ← endpoint.setAddress("/")
      policies         ← Resource @Path
              /P001    ← method @Path

GET /rest/policies/P001
~~~

CXF starter 會配置 CXFServlet 與預設 SpringBus。這裡注入它提供的 Bus，不需要自己再建一份，也不需要應用維護 web.xml。CXF starter 內部可能載入自身 XML，與應用使用 Java configuration 並不衝突。[CXF 4.2.3 自動配置原始碼](https://github.com/apache/cxf/blob/cxf-4.2.3/integration/spring-boot/autoconfigure/src/main/java/org/apache/cxf/spring/boot/autoconfigure/CxfAutoConfiguration.java)。

JAXRSServerFactoryBean 的 setServiceBeans、setProviders 與 create 用法可對照 [CXF 4.2.3 官方範例](https://github.com/apache/cxf/blob/cxf-4.2.3/distribution/src/main/release/samples/jax_rs/spring_boot/src/main/java/sample/rs/service/SampleRestApplication.java)。應使用 Spring 注入的 Resource，讓其 Service 依賴與代理保留；Server 關閉時呼叫 destroy。[Server API 原始碼](https://github.com/apache/cxf/blob/cxf-4.2.3/core/src/main/java/org/apache/cxf/endpoint/Server.java)。

### 9.3 掛 endpoint 或 Bus 的差別

- 掛 endpoint：只套用到這個 Server 的 Resources；本文採這種方式。
- 掛 Bus：套用到使用該 Bus 的端點，可能同時包含其他 REST 或 SOAP endpoint。
- 同一個 interceptor 不要同時掛兩邊；保留它的 phase、先後順序與適用協定。
- Gson／JSON MessageBodyReader、MessageBodyWriter 和 ExceptionMapper 屬於 JAX-RS providers，加入 setProviders。Spring MVC 的 JSON 設定不會因此等同於 CXF 的 provider 設定。
- RequestValidationInterceptor、ResponseHeaderInterceptor、UnifiedFaultInterceptor 是否也需加入，應依原系統的有效配置決定。不要因共用 JAR 包含某類別，就在所有服務強制啟用它。

CXF 也提供 component-scan 的端點自動發現方式；本文刻意只示範明確 Server 配置，方便控制選定 API 的範圍。查閱 4.2.3 原始碼時發現，其自動掃描 server 屬性為 cxf.jaxrs.server.address，官網部分舊教學仍寫 path；本文用 setAddress 避開該落差。[實際屬性原始碼](https://github.com/apache/cxf/blob/cxf-4.2.3/rt/frontend/jaxrs/src/main/java/org/apache/cxf/jaxrs/spring/AbstractSpringConfigurationFactory.java)。

## 10. 外部 JAR：怎麼註冊，以及 Connection 怎麼傳

### 10.1 升級後 JAR 已有元件註解

可以另設一個 configuration，精確掃描 JAR 自己的套件：

~~~java
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = {
    "com.external.liba.service",
    "com.external.liba.dao",
    "com.external.libb.service",
    "com.external.libb.dao"
})
public class LegacyLibrariesConfiguration {
}
~~~

若它位於本服務 config package，會被啟動類掃到。若位於共用 JAR，則 @Import 它。這適用普通、明確選用的 configuration；後述 Boot auto-configuration 另有不應 component-scan 的規範。

### 10.2 無元件註解，或要更嚴格控制暴露的 Bean

用 @Bean 建立。下面以「升級後 A Service 提供 DAO 建構子」為假設：

~~~java
@Configuration(proxyBeanMethods = false)
public class LegacyLibrariesConfiguration {
    @Bean
    LegacyAS400Dao legacyAS400Dao() {
        return new LegacyAS400Dao();
    }

    @Bean
    LegacyAS400Service legacyAS400Service(LegacyAS400Dao dao) {
        return new LegacyAS400Service(dao);
    }

    @Bean
    AnotherLegacyDao anotherLegacyDao() {
        return new AnotherLegacyDao();
    }
}
~~~

如果實際 JAR 只有無參數建構子與 setter，改用其真正提供的 setter；若欄位使用可被 Spring 7 處理的注入註解，@Bean 建立後也會進行注入。原模擬版本使用 javax.annotation.Resource，升級到 Jakarta 世代時要核實註解已相容。

**@Bean 不會憑空修復舊 JAR 不相容的註解或方法。** 先完成 JAR 升級，再選這兩種註冊方式之一。

### 10.3 使用 Spring 交易的目標寫法

以下是第 11 節雙 DataSource 配置下，將外部 JAR JDBC 操作交 Spring 統一控管的示例：

~~~java
import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacyCustomerService {
    private final DataSource legacyDataSource;
    private final LegacyAS400Service libA;
    private final AnotherLegacyDao libB;

    public LegacyCustomerService(
            @Qualifier("legacyDataSource") DataSource legacyDataSource,
            LegacyAS400Service libA,
            AnotherLegacyDao libB) {
        this.legacyDataSource = legacyDataSource;
        this.libA = libA;
        this.libB = libB;
    }

    @Transactional(
        transactionManager = "legacyTransactionManager",
        rollbackFor = Exception.class
    )
    public void addThenDisable(String name) throws Exception {
        Connection conn = DataSourceUtils.getConnection(legacyDataSource);
        try {
            libA.addCustomer(conn, name);
            libB.updateCustomerStatus(conn, name, "INACTIVE");
        } finally {
            DataSourceUtils.releaseConnection(conn, legacyDataSource);
        }
    }
}
~~~

addThenDisable 只是示範同一交易呼叫兩個 JAR，並非建議公司的業務順序。方法名稱與参数對應模擬 JAR；import 使用升級後實際 package。

此路線的前提：

1. 從另一個 Bean 呼叫這個 Service，讓交易代理生效。
2. DataSourceUtils 使用與 transaction manager 相同的 DataSource Bean，取得交易綁定的 Connection。
3. JAR 只關閉自己建立的 Statement／ResultSet，借用的 Connection 交管理者處理。
4. 此流程不手動 commit、rollback、setAutoCommit，也不直接 close 該 Connection；讓例外傳出，Spring 才能依 rollback 規則處理。

releaseConnection 會識別交易綁定，並非無條件把仍在交易中的連線關閉。[DataSourceUtils API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/datasource/DataSourceUtils.html)。MyBatis 參與同一交易也要求其 factory 與 transaction manager 使用對應的 DataSource。[MyBatis 交易整合](https://mybatis.org/spring/transactions.html)。

**保留原本手動交易也是可行的過渡路線。** 若要保留主程式自行取得連線、commit／rollback／close 的行為，就把它當成獨立的手動交易範圍；不能只在外面加 @Transactional，就假設原本直接 getConnection 的程式已加入 Spring 交易。選定交易管理方式後，逐条驗證寫入與回滾。

對 AS400 物件，上例不適用。它仍可以由升級後的共用整合類別／連線池管理，再於呼叫時傳给 JAR，但其借還、主機程式呼叫與 commitment control 必須依實際 API 決定。JDBC 的 @Transactional 不會因為同一方法也傳了 AS400 物件，就自動包含主機操作。[IBM ProgramCall](https://www.ibm.com/docs/en/i/7.5.0?topic=classes-programcall-class)。

## 11. 雙 DataSource：不要讓注入猜資料庫

假設本服務需要：

- bankDataSource：業務 MyBatis SQL。
- legacyDataSource：外部 JAR 的直接 JDBC。

第二組既然只用 JDBC，**不必為它額外建 SqlSessionFactory**。真的需要第二組 MyBatis 時，再加對應 factory／template／MapperScan。

~~~yaml
app:
  datasource:
    bank:
      url: ${BANK_DB_URL}
      username: ${BANK_DB_USER}
      password: ${BANK_DB_PASSWORD}
      pool:
        maximum-pool-size: 10
    legacy:
      url: ${LEGACY_DB_URL}
      username: ${LEGACY_DB_USER}
      password: ${LEGACY_DB_PASSWORD}
      pool:
        maximum-pool-size: 5
~~~

以下 configuration **替代**第 7 節的單一資料來源設定與 MapperConfiguration，不要同時貼入兩套：

~~~java
package com.company.bankapi.config;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
@MapperScan(
    basePackages = "com.company.bankapi.bank.service.dao",
    annotationClass = Mapper.class,
    sqlSessionTemplateRef = "bankSqlSessionTemplate"
)
public class BankDataConfiguration {
    @Bean
    @Primary
    @ConfigurationProperties("app.datasource.bank")
    public DataSourceProperties bankDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("app.datasource.bank.pool")
    public HikariDataSource bankDataSource(
            @Qualifier("bankDataSourceProperties") DataSourceProperties p) {
        return p.initializeDataSourceBuilder()
            .type(HikariDataSource.class).build();
    }

    @Bean
    @ConfigurationProperties("app.datasource.legacy")
    public DataSourceProperties legacyDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.datasource.legacy.pool")
    public HikariDataSource legacyDataSource(
            @Qualifier("legacyDataSourceProperties") DataSourceProperties p) {
        return p.initializeDataSourceBuilder()
            .type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public SqlSessionFactory bankSqlSessionFactory(
            @Qualifier("bankDataSource") DataSource ds) throws Exception {
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(ds);
        factory.setVfs(SpringBootVFS.class);
        var resolver = new PathMatchingResourcePatternResolver();
        factory.setMapperLocations(
            resolver.getResources("classpath*:mapper/bank/**/*.xml"));

        var options = new org.apache.ibatis.session.Configuration();
        options.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(options);
        return factory.getObject();
    }

    @Bean
    @Primary
    public SqlSessionTemplate bankSqlSessionTemplate(
            @Qualifier("bankSqlSessionFactory") SqlSessionFactory factory) {
        return new SqlSessionTemplate(factory);
    }

    @Bean
    public JdbcTemplate bankJdbcTemplate(
            @Qualifier("bankDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public JdbcTemplate legacyJdbcTemplate(
            @Qualifier("legacyDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    @Primary
    public DataSourceTransactionManager bankTransactionManager(
            @Qualifier("bankDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean
    public DataSourceTransactionManager legacyTransactionManager(
            @Qualifier("legacyDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }
}
~~~

这里有三个需要跟示例一起调整的地方：

1. 如果沿用第 8 節 AuditLogDao，并且稽核表放銀行資料庫，在 bankSqlSessionFactory 再載入 mapper/company-audit 下的 XML；AuditStorageConfiguration 的參數加上 @Qualifier("bankSqlSessionTemplate")。目前上述 factory 僅載入業務 SQL，刻意不預設稽核落在哪個 DB。
2. 注入 JdbcTemplate／SqlSessionTemplate 的普通 DAO，在多個同類 Bean 存在時加入對應 @Qualifier。@Primary 是預設選擇，不代表其他資料來源不用選。
3. 某個業務寫入若由銀行資料庫控管，使用 @Transactional(transactionManager = "bankTransactionManager")；外部 JDBC 路線使用 legacyTransactionManager。

Boot 4 的 DataSourceProperties package 是 **org.springframework.boot.jdbc.autoconfigure**，不要照抄 Boot 3 的舊 import。此配置借助它處理 url 與 Hikari jdbc-url 的差異。[Boot 4 DataSource Java 配置](https://docs.spring.io/spring-boot/how-to/data-access.html)。

手動 factory 加入 SpringBootVFS，是為了 executable JAR 中的類別／alias／handler 掃描；MyBatis starter 在自動建立 factory 時會處理，手動配置需自行設定。[MyBatis 手動 factory 的 VFS 說明](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)。

若兩個資料來源都用 MyBatis，為 legacy 加上 legacySqlSessionFactory、legacySqlSessionTemplate，再以另一個 @MapperScan 指向 legacy 的專用 mapper package 與 template。不要讓同一 Mapper 被兩個 scanner 重複接管。

**兩個 DataSourceTransactionManager 是兩個本地交易管理器，並不自動形成跨庫原子交易。** 服務拆成不同 JVM 後，也不會延續原本同一 Connection 的呼叫鏈。需依真正「必須一起成功／失敗」的業務另定交易範圍，而非用 scan 解決。[Spring 交易資源參與方式](https://docs.spring.io/spring-framework/reference/data-access/transaction/strategies.html)。

## 12. 共用 DAO 要不要拆出去

技術上可以。DAO class、Mapper 介面和 SQL XML 都可以放 JAR，再以第 7、8、11 節方式註冊。**但是否應共用，要看它代表什麼責任。**

| DAO 內容 | 目前建議 |
|---|---|
| 某機構自己的業務表／業務規則 | 與該功能服務一起搬，歸該服務管理 |
| 確定統一的稽核表、技術紀錄 | 可作可選的共用模組；由服務選擇資料來源及失敗策略 |
| 只包裝取連線／釋放連線的工具 | 可重用程式，但改由注入的 DataSource 決定來源；避免固定 JNDI 名稱 |
| 多機構共同修改的核心業務表 | 先確認資料責任與共同交易，不宜因檔名叫 CommonDao 就直接全面共用 |
| AS400／外部 JAR adapter | 可以封裝一致的呼叫及資源管理；各服務是否直接使用它另依業務範圍決定 |

過渡期間多個服務共用資料庫是可能的，但要明確列出誰讀、誰寫、誰維護 schema。共用 DAO JAR 只共用程式碼，不能消除共用資料表帶來的相依，也不能取代跨服務呼叫契約。

## 13. 設定值、有依賴的 Utils 與其他共用功能

### 13.1 用 ConfigurationProperties 取代零散靜態設定

下面只示範設定綁定，沒有改變實際檔案業務：

~~~java
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("company.files")
public record FileProperties(
        String uploadDir,
        @DefaultValue("10485760") long maxBytes) {
}
~~~

~~~java
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FileProperties.class)
public class FileConfiguration {
    @Bean
    public FileStorageService fileStorageService(FileProperties properties) {
        return new FileStorageService(
            properties.uploadDir(), properties.maxBytes());
    }
}
~~~

FileStorageService 使用模擬專案已有的 String、long 建構子。這是明確 @Bean 註冊方式；若同一類別還保留 @Service，應避免再被掃描產生第二個 Bean。

~~~yaml
company:
  files:
    upload-dir: ${UPLOAD_DIR}
    max-bytes: 10485760
~~~

Record 是縮短示例的寫法，也可用普通 getter／setter JavaBean。@EnableConfigurationProperties 用來註冊並綁定設定物件；把它放普通 JAR 也可工作。[Boot 外部設定](https://docs.spring.io/spring-boot/reference/features/external-config.html)。

純日期／字串 Utils 可以保持 static；需要檔案路徑、FTP client、MailSender 或 HTTP client 的類別，則用 @Component 或 @Bean 注入。不要在共享 singleton 欄位保留每次請求的 Connection、AS400 借用物件或使用者資料。

### 13.2 原本 DB system_properties 的情況

ConfigurationProperties 綁定的是 Environment 中的設定，不會自動查 system_properties。原本的 DatabasePropertyPlaceholderConfigurer 要分辨兩類需求：

- 啟動必需的 DB 連線／憑證／基礎端點：先提供於該服務的啟動設定。
- 業務用系統參數：可以由 SystemParameterService／DAO 讀取，再注入使用者。

若 DB 參數必須在其他 Bean 建立前變成 Environment 的值，就要另設早期設定載入機制，例如合適的 ConfigData 整合。不能把原邏輯隨便移到 @PostConstruct，就假設已建立的 client／Bean 會重新綁定。這部分需依公司的參數使用時序另做小範圍設計。

### 13.3 Filter：替代 web.xml 的 mapping／init-param

下面設定模擬專案的字型檢查，Filter 本身需已升級使用 jakarta.servlet：

~~~java
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration(proxyBeanMethods = false)
public class ServletFilterConfiguration {
    @Bean
    public FontCheckFilter fontCheckFilter() {
        return new FontCheckFilter();
    }

    @Bean
    public FilterRegistrationBean<FontCheckFilter> fontFilterRegistration(
            FontCheckFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/rest/*");
        registration.addInitParameter("targetFont", "標楷體");
        registration.setOrder(10);
        return registration;
    }
}
~~~

Filter 與 registration 在同一配置管理，不再另外使用 @WebFilter／@ServletComponentScan 註冊同一個 Filter。映射改成 /rest/* 是示例；若需保持原 /*，應照契約設定。[Boot Servlet Filter 註冊](https://docs.spring.io/spring-boot/reference/web/servlet.html)。

### 13.4 Mail、FTP、HTTP、排程

| 功能 | 配置方向 |
|---|---|
| JavaMailService | 自身用 @Service／@Bean；加入 spring-boot-starter-mail 並配置 spring.mail，注入 JavaMailSender |
| FTP 工具 | 無需硬做 starter；用 @Bean／@Component＋設定物件，保留正確的每次操作連線生命週期 |
| HTTP 工具 | 以 @Bean 建立相容的 client，注入呼叫工具；timeout／關閉行為在配置中明確設定 |
| Spring 排程 | 指定需要的服務載入工作 Bean，再 @EnableScheduling；不可把所有排程無條件放進每個共用 starter |
| Quartz | 使用 spring-boot-starter-quartz，建立 JobDetail／Trigger Bean；是否 JDBC JobStore／cluster 另外配置 |
| 背景執行 | 若採 @Async，需 @EnableAsync 與 executor；換成註解本身不會提供工作持久化、重啟恢復或去重 |

[Boot Mail](https://docs.spring.io/spring-boot/reference/io/email.html)、[Boot Quartz](https://docs.spring.io/spring-boot/reference/io/quartz.html)、[Spring scheduling／async](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)。

## 14. 設定穩定後，如何做成自訂 starter

先以一個 Response interceptor 示範最小的自動註冊。CompanyResponseInterceptor 是公司自行提供、具有無參數建構子的相容 CXF interceptor；此節示範載入機制，並非另一份完整稽核實作。

~~~java
package com.company.cxf.autoconfigure;

import com.company.shared.interceptor.CompanyResponseInterceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(JAXRSServerFactoryBean.class)
public class CompanyCxfAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(CompanyResponseInterceptor.class)
    public CompanyResponseInterceptor companyResponseInterceptor() {
        return new CompanyResponseInterceptor();
    }
}
~~~

在提供 auto-configuration 的 JAR 內加入這個檔案：

~~~text
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
~~~

內容一行：

~~~text
com.company.cxf.autoconfigure.CompanyCxfAutoConfiguration
~~~

使用端加入 starter dependency，starter 再帶入這個 auto-configuration JAR 與必要依賴。Boot 讀取 imports 後載入配置；@ConditionalOnMissingBean 讓服務自己定義同類 Bean 時，預設實作退讓。[Boot 官方自動配置機制](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)。

這個最小示例只完成 Bean 註冊，**仍需掛到第 9 節 endpoint**：

~~~java
// companyResponseInterceptor 是 @Bean 方法參數注入的物件。
endpoint.getOutInterceptors().add(companyResponseInterceptor);
~~~

若重複的掛載也想共用，可讓 common／starter 提供 CompanyCxfConfigurer，內部統一加入公司 interceptor／providers；每個服務建立 Server 時呼叫一次：

~~~java
// configurer.apply(endpoint) 是公司自行定義的 helper，並非 CXF 內建 API。
// 在 helper 中執行第 9 節的共同掛載，然後再 endpoint.create()。
~~~

若規範要求「dependency 加入後，所有 CXF 端點自動套用」，完整 starter 就要另實作 Bus 層掛載與生命週期整合，並驗證不重複掛載。是否全域套用 REST／SOAP、如何覆寫／停用，需要先定義。不能把 starter 命名完成就視為已實現這個功能。

Auto-configuration 應透過 imports 載入，放在不會被應用 scan 的 package；其中用明確 @Bean／@Import，避免再開 @ComponentScan。普通 @Import 路線升級為 auto-configuration 時，要移除同一套元件的舊匯入／掃描，避免重複註冊。需要參數化時，再結合第 13 節的 ConfigurationProperties。

## 15. 原本 XML 到 Java configuration 的對照

| 舊配置／行為 | 新服務的對應 |
|---|---|
| web.xml 的 ContextLoaderListener | SpringApplication.run 建立應用 context |
| contextConfigLocation 載入主 XML／外部 JAR XML | @Import configuration；或明確選擇 auto-configuration |
| context:component-scan | 啟動類預設 scan，必要時精確 @ComponentScan |
| bean／constructor-arg／property | @Bean 方法、建構子與方法參數注入 |
| jee:jndi-lookup | 一般獨立程序用服務自己的 DataSource 配置；保留外部容器 JNDI 時才使用對應 lookup |
| SqlSessionFactoryBean／Template | 單資料來源可由 MyBatis starter 提供，多資料來源明確 @Bean |
| MapperScannerConfigurer | @MapperScan，必要時指定 annotationClass／sqlSessionTemplateRef |
| tx:annotation-driven | Boot 的交易自動配置；手動配置示例用 @EnableTransactionManagement |
| jaxrs:server／serviceBeans／providers | JAXRSServerFactoryBean、setServiceBeans、setProviders |
| cxf:bus 的 interceptor 配置 | 注入 Bus 掛載，或依本文移為明確 endpoint 配置 |
| servlet/filter mapping | CXF starter 的 cxf.path／FilterRegistrationBean |
| task:annotation-driven | @EnableScheduling／@EnableAsync，依實際需要啟用 |
| mailSender bean | Boot Mail 自動配置，或自己 @Bean 定義 |
| GlobalConfig static 值 | 優先用配置物件／服務注入；每個應用有自己的設定 |

Spring Boot 4 仍可使用 @ImportResource 過渡載入 Spring XML；「採 Java configuration」不等於框架已禁止 XML。已升級 JAR 若暫時仍只有 XML，可以逐段移轉，不需要同時重寫所有配置。[Spring 混合 Java／XML 配置](https://docs.spring.io/spring-framework/reference/core/beans/java/composing-configuration-classes.html)。

javax.sql.DataSource 與 java.sql.Connection 屬於 Java SE，仍保留原 package；不要將所有 javax.* 全域替換成 jakarta.*。

## 16. 第一個服務可依序這樣落地

1. **確認 API 清單與原契約。** 列出 paths、methods、headers、JSON／XML、錯誤碼、原 interceptor 與來源機構。
2. **把這組 API 的業務鏈搬入新服務。** 包含 Resource、Service／impl、DAO／impl、VO、SQL；先盤點跨機構呼叫及必須共用的交易。
3. **建立 Boot 啟動類與相容依賴。** 保留 JAX-RS，明確採用 CXF JAX-RS starter。
4. **設定該服務真的需要的資料來源。** 單一來源先用 auto-configuration；多來源按第 11 節逐一綁定。
5. **普通共用 JAR＋@Import。** 先搬真正共用的 interceptor、provider、工具；共用 DAO 依第 12 節決定範圍。
6. **發布選定的 JAX-RS Resources。** Java configuration 中明確設定 server、providers、interceptors。
7. **接入升級後外部 JAR。** 確認每種連線的管理者、異常傳播與交易方式；AS400 與 JDBC 分別驗證。
8. **配置其餘資源。** 確認檔案目錄、參數、郵件、HTTP、FTP 和排程歸屬；每個服務／副本不要重複觸發同一業務工作。
9. **用真正的 HTTP 與 DB 測試驗證。** 至少驗證路由與契約、interceptor 只執行預期次數、Mapper 使用正確資料來源、跨 JAR 寫入失敗回滾、連線歸還及關閉生命週期。
10. **共同設定穩定後再抽 starter。** 服務保留 API 與業務選擇，starter 提供一致且可覆寫的技術配置。

前幾個服務先做清楚的共同配置，比一開始設計大量 starter 更容易辨認真正的共用範圍。本文的配置示例需要在新服務中完成上述驗證，才是可上線的遷移成果。
