package com.example.cxfdemo.config;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigSingletonTest {

    @Before
    public void setUp() {
        ConfigSingleton.getInstance().clear();
    }

    @Test
    public void testNeedInitAndClearAndReload() {
        ConfigSingleton instance = ConfigSingleton.getInstance();
        assertTrue("Clear 後 needInit 應回傳 true", instance.needInit());

        // 手動模擬寫入資料
        instance.getMap().put("test.key", "test.value");
        assertFalse("放入資料後 needInit 應回傳 false", instance.needInit());
        assertEquals("test.value", ConfigSingleton.getMappingData("test.key"));

        // 測試 clear
        instance.clear();
        assertTrue("clear 後 needInit 應再度為 true", instance.needInit());
        assertNull("clear 後查無 key 應回傳 null", instance.getMap().get("test.key"));
    }

    @Test
    public void testGetMappingDataFallback() {
        ConfigSingleton instance = ConfigSingleton.getInstance();
        instance.getMap().put("mail.from", "service@company.com");

        String mailFrom = ConfigSingleton.getMappingData("mail.from");
        assertEquals("service@company.com", mailFrom);
    }
}
