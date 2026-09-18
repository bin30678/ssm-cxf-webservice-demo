package com.example.cxfdemo.service;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ExternalApiService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiService.class);

    /**
     * 模擬呼叫外部 API
     */
    public String callExternalApi() {
        String url = "https://jsonplaceholder.typicode.com/posts/1";
        log.info("ExternalApiService.callExternalApi calling URL: {}", url);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(entity, "UTF-8");
                log.info("ExternalApiService.callExternalApi success");
                return result;
            }
        } catch (Exception e) {
            log.error("ExternalApiService.callExternalApi failed for URL: {}", url, e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error("Failed to close http response", e);
                }
            }
            try {
                httpClient.close();
            } catch (IOException e) {
                log.error("Failed to close httpClient", e);
            }
        }
        return "ERROR";
    }
}
