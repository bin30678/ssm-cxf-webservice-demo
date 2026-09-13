package com.example.cxfdemo.ws;

import com.example.cxfdemo.model.PolicyInfo;
import com.example.cxfdemo.service.PolicyService;
import javax.annotation.Resource;
import javax.jws.WebService;
import org.springframework.stereotype.Service;

@Service
@WebService(
        endpointInterface = "com.example.cxfdemo.ws.PolicyInquiryPort",
        serviceName = "PolicyInquiryService",
        portName = "PolicyInquiryPort",
        targetNamespace = "https://example.com/cxfdemo/policy")
public class PolicyInquiryPortImpl implements PolicyInquiryPort {
    @Resource
    private PolicyService policyService;

    public PolicyInquiryPortImpl() {
    }

    public PolicyInquiryPortImpl(PolicyService policyService) {
        this.policyService = policyService;
    }

    @Override
    public String getPolicyStatus(String policyNo) {
        PolicyInfo policy = policyService.find(policyNo);
        return policy == null ? "NOT_FOUND" : policy.getStatus();
    }

    @Override
    public PolicyInfo getPolicySummary(String policyNo) {
        return policyService.find(policyNo);
    }
}
