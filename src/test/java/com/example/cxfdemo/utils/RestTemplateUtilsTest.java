package com.example.cxfdemo.utils;

import com.example.cxfdemo.dao.ExternalApiLogDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Date;

public class RestTemplateUtilsTest {

    private RestTemplateUtils restTemplateUtils;
    private ExternalApiLogDao mockDao;

    @Before
    public void setup() throws Exception {
        restTemplateUtils = new RestTemplateUtils();

        mockDao = Mockito.mock(ExternalApiLogDao.class);

        Field daoField = RestTemplateUtils.class.getDeclaredField("externalApiLogDao");
        daoField.setAccessible(true);
        daoField.set(restTemplateUtils, mockDao);

        restTemplateUtils.init();
    }

    @Test
    public void testGetWithLog_shouldCallExternalApiAndLogToDao() {
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
    }
}
