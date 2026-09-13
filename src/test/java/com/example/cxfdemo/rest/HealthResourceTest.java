package com.example.cxfdemo.rest;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class HealthResourceTest {

    @Test
    public void testHealth() {
        HealthResource healthResource = new HealthResource();
        Map<String, String> result = healthResource.health();

        Assert.assertNotNull(result);
        Assert.assertEquals("UP", result.get("status"));
        Assert.assertEquals("ssm-cxf-webservice-demo", result.get("service"));
        Assert.assertNotNull(result.get("time"));
    }
}
