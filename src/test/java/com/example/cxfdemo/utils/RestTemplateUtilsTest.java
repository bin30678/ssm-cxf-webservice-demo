package com.example.cxfdemo.utils;

import com.example.cxfdemo.dao.ExternalApiLogDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.Date;

public class RestTemplateUtilsTest {

    private RestTemplateUtils restTemplateUtils;
    private ExternalApiLogDao mockDao;
    private MockRestServiceServer server;

    @Before
    public void setup() throws Exception {
        restTemplateUtils = new RestTemplateUtils();

        mockDao = Mockito.mock(ExternalApiLogDao.class);

        Field daoField = RestTemplateUtils.class.getDeclaredField("externalApiLogDao");
        daoField.setAccessible(true);
        daoField.set(restTemplateUtils, mockDao);

        restTemplateUtils.init();
        Field templateField = RestTemplateUtils.class.getDeclaredField("restTemplate");
        templateField.setAccessible(true);
        server = MockRestServiceServer.createServer((RestTemplate) templateField.get(restTemplateUtils));
    }

    @Test
    public void testGetWithLog_shouldCallExternalApiAndLogToDao() {
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://httpbin.org/get"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "{\"url\":\"https://httpbin.org/get\"}", MediaType.APPLICATION_JSON));
        String result = restTemplateUtils.getWithLog("https://httpbin.org/get");

        Assert.assertNotNull("回傳結果不應為 null", result);
        Assert.assertTrue("回傳結果應包含 url 欄位", result.contains("\"url\""));

        // 驗證 DAO 的 insertApiLog 在 finally 區塊中被呼叫
        Mockito.verify(mockDao, Mockito.times(1)).insertApiLog(
                Mockito.eq("https://httpbin.org/get"),
                Mockito.isNull(String.class),
                Mockito.anyString(),
                Mockito.isNull(String.class),
                Mockito.any(Date.class)
        );
        server.verify();
    }
}
