package com.example.cxfdemo.ws;

import com.example.cxfdemo.model.PolicyInfo;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(name = "PolicyInquiryPort", targetNamespace = "https://example.com/cxfdemo/policy")
public interface PolicyInquiryPort {
    @WebMethod
    String getPolicyStatus(@WebParam(name = "policyNo") String policyNo);

    @WebMethod
    PolicyInfo getPolicySummary(@WebParam(name = "policyNo") String policyNo);
}
