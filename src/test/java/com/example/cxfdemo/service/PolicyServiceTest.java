package com.example.cxfdemo.service;

import com.example.cxfdemo.mapper.DemoMapper;
import com.example.cxfdemo.model.PolicyInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

public class PolicyServiceTest {

    private DemoMapper mockMapper;
    private PolicyService policyService;

    @Before
    public void setUp() {
        mockMapper = Mockito.mock(DemoMapper.class);
        policyService = new PolicyService(mockMapper);
    }

    @Test
    public void testFind_shouldReturnPolicy() {
        PolicyInfo mockPolicy = new PolicyInfo();
        mockPolicy.setPolicyNo("POL123456");
        mockPolicy.setHolderName("王大明");
        mockPolicy.setStatus("ACTIVE");

        Mockito.when(mockMapper.findPolicy("POL123456")).thenReturn(mockPolicy);

        PolicyInfo result = policyService.find("POL123456");
        Assert.assertNotNull(result);
        Assert.assertEquals("POL123456", result.getPolicyNo());
        Assert.assertEquals("王大明", result.getHolderName());
        Assert.assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    public void testFindAll_shouldReturnAllPolicies() {
        PolicyInfo p1 = new PolicyInfo();
        p1.setPolicyNo("POL001");
        PolicyInfo p2 = new PolicyInfo();
        p2.setPolicyNo("POL002");

        Mockito.when(mockMapper.findPolicies()).thenReturn(Arrays.asList(p1, p2));

        List<PolicyInfo> result = policyService.findAll();
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("POL001", result.get(0).getPolicyNo());
        Assert.assertEquals("POL002", result.get(1).getPolicyNo());
    }
}
