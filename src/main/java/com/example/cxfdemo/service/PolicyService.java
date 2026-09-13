package com.example.cxfdemo.service;

import java.util.List;
import com.example.cxfdemo.mapper.DemoMapper;
import com.example.cxfdemo.model.PolicyInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class PolicyService {
    @Resource
    private DemoMapper mapper;

    public PolicyService() {
    }

    public PolicyService(DemoMapper mapper) {
        this.mapper = mapper;
    }

    public PolicyInfo find(String policyNo) {
        return mapper.findPolicy(policyNo);
    }

    public List<PolicyInfo> findAll() {
        return mapper.findPolicies();
    }

    public int createPolicy(PolicyInfo policy) {
        return mapper.insertPolicy(policy);
    }

    public int updatePolicy(PolicyInfo policy) {
        return mapper.updatePolicy(policy);
    }
}
