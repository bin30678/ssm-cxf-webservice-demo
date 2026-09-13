package com.example.cxfdemo.utils;

import com.example.cxfdemo.dao.ExternalApiLogDao;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import javax.annotation.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;
import java.util.Date;

@Component
public class RestTemplateUtils {

    @Resource
    private ExternalApiLogDao externalApiLogDao;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        try {
            // ?��??�?��?證�? TrustStrategy
            TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) {
                    return true;
                }
            };

            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, acceptingTrustStrategy)
                    .build();

            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(csf)
                    .build();

            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);

            restTemplate = new RestTemplate(requestFactory);

        } catch (Exception e) {
            e.printStackTrace();
            // 如�?失�?，退?��?設�? RestTemplate
            restTemplate = new RestTemplate();
        }
    }

    /**
     * ?��?POST 請�?並�?紀?�寫??DB
     */
    public String postWithLog(String url, String requestBody, HttpHeaders headers) {
        String responseBody = null;
        String exceptionMsg = null;
        Date callTime = new Date();

        try {
            HttpEntity<String> entity = new HttpEntity<String>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            responseBody = response.getBody();
            return responseBody;
        } catch (Exception e) {
            exceptionMsg = e.getMessage();
            throw e;
        } finally {
            if (externalApiLogDao != null) {
                externalApiLogDao.insertApiLog(url, requestBody, responseBody, exceptionMsg, callTime);
            }
        }
    }

    /**
     * ?��?GET 請�?並�?紀?�寫??DB
     */
    public String getWithLog(String url) {
        String responseBody = null;
        String exceptionMsg = null;
        Date callTime = new Date();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            responseBody = response.getBody();
            return responseBody;
        } catch (Exception e) {
            exceptionMsg = e.getMessage();
            throw e;
        } finally {
            if (externalApiLogDao != null) {
                externalApiLogDao.insertApiLog(url, null, responseBody, exceptionMsg, callTime);
            }
        }
    }
}
