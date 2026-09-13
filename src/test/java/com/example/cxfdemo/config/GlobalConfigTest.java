package com.example.cxfdemo.config;

import org.junit.Assert;
import org.junit.Test;

public class GlobalConfigTest {

    @Test
    public void testSetAndGetUrl() {
        String testUrl = "http://test-url.com";
        GlobalConfig.setUrl(testUrl);
        Assert.assertEquals("The URL should match what was set", testUrl, GlobalConfig.getUrl());
    }
}
