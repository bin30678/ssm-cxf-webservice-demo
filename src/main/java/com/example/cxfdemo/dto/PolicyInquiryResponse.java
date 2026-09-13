package com.example.cxfdemo.dto;

import com.example.cxfdemo.model.PolicyInfo;

public class PolicyInquiryResponse {
    private final boolean success;
    private final PolicyInfo policy;
    private final String channel;

    public PolicyInquiryResponse(boolean success, PolicyInfo policy, String channel) {
        this.success = success;
        this.policy = policy;
        this.channel = channel;
    }

    public boolean isSuccess() { return success; }
    public PolicyInfo getPolicy() { return policy; }
    public String getChannel() { return channel; }
}
