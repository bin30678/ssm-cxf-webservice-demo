package com.example.cxfdemo.ws;

import com.example.cxfdemo.model.PolicyInfo;
import com.example.cxfdemo.service.PolicyService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PolicyInquiryPortImplTest {

    private PolicyService mockPolicyService;
    private PolicyInquiryPortImpl policyInquiryPort;

    @Before
    public void setUp() {
        mockPolicyService = Mockito.mock(PolicyService.class);
        policyInquiryPort = new PolicyInquiryPortImpl(mockPolicyService);
    }

    @Test
    public void testGetPolicyStatus_found() {
        PolicyInfo policy = new PolicyInfo();
        policy.setPolicyNo("P123");
        policy.setStatus("ACTIVE");

        Mockito.when(mockPolicyService.find("P123")).thenReturn(policy);

        String status = policyInquiryPort.getPolicyStatus("P123");
        Assert.assertEquals("ACTIVE", status);
    }

    @Test
    public void testGetPolicyStatus_notFound() {
        Mockito.when(mockPolicyService.find("P999")).thenReturn(null);

        String status = policyInquiryPort.getPolicyStatus("P999");
        Assert.assertEquals("NOT_FOUND", status);
    }

    @Test
    public void testGetPolicySummary() {
        PolicyInfo policy = new PolicyInfo();
        policy.setPolicyNo("P123");
        policy.setHolderName("張大春");

        Mockito.when(mockPolicyService.find("P123")).thenReturn(policy);

        PolicyInfo summary = policyInquiryPort.getPolicySummary("P123");
        Assert.assertNotNull(summary);
        Assert.assertEquals("張大春", summary.getHolderName());
    }
}
