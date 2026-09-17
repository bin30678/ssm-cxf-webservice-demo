package com.example.cxfdemo.dao;

import com.example.cxfdemo.model.PolicyInfo;

public interface PolicyDao {

    PolicyInfo findPolicyViaMapper(String policyNo);

    PolicyInfo findPolicyViaJdbc(String policyNo);

    PolicyInfo findPolicyViaPureJdbc(String policyNo) throws Exception;

    int insertPolicy(PolicyInfo policy);

    int updatePolicyStatus(String policyNo, String status);
}
