package com.example.cxfdemo.ws;

import com.example.cxfdemo.model.PolicyInfo;
import com.example.cxfdemo.service.PolicyService;
import javax.annotation.Resource;
import javax.jws.WebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@WebService(
        endpointInterface = "com.example.cxfdemo.ws.PolicyInquiryPort",
        serviceName = "PolicyInquiryService",
        portName = "PolicyInquiryPort",
        targetNamespace = "https://example.com/cxfdemo/policy")
public class PolicyInquiryPortImpl implements PolicyInquiryPort {

    private static final Logger log = LoggerFactory.getLogger(PolicyInquiryPortImpl.class);

    @Resource
    private PolicyService policyService;

    public PolicyInquiryPortImpl() {
    }

    public PolicyInquiryPortImpl(PolicyService policyService) {
        this.policyService = policyService;
    }

    @Override
    public String getPolicyStatus(String policyNo) {
        log.info("PolicyInquiryPortImpl.getPolicyStatus called for policyNo: {}", policyNo);
        PolicyInfo policy = policyService.find(policyNo);
        String status = policy == null ? "NOT_FOUND" : policy.getStatus();
        log.info("PolicyInquiryPortImpl.getPolicyStatus result: {}", status);
        return status;
    }

    @Override
    public PolicyInfo getPolicySummary(String policyNo) {
        log.info("PolicyInquiryPortImpl.getPolicySummary called for policyNo: {}", policyNo);
        return policyService.find(policyNo);
    }
}
