package com.example.cxfdemo.service;

import java.util.List;
import com.example.cxfdemo.mapper.DemoMapper;
import com.example.cxfdemo.model.PolicyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class PolicyService {

    private static final Logger log = LoggerFactory.getLogger(PolicyService.class);

    @Resource
    private DemoMapper mapper;

    public PolicyService() {
    }

    public PolicyService(DemoMapper mapper) {
        this.mapper = mapper;
    }

    public PolicyInfo find(String policyNo) {
        log.info("PolicyService.find called for policyNo: {}", policyNo);
        return mapper.findPolicy(policyNo);
    }

    public List<PolicyInfo> findAll() {
        log.info("PolicyService.findAll called");
        return mapper.findPolicies();
    }

    public int createPolicy(PolicyInfo policy) {
        log.info("PolicyService.createPolicy called for policy: {}", policy);
        return mapper.insertPolicy(policy);
    }

    public int updatePolicy(PolicyInfo policy) {
        log.info("PolicyService.updatePolicy called for policy: {}", policy);
        return mapper.updatePolicy(policy);
    }
}
