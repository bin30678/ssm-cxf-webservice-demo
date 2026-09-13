package com.example.cxfdemo.dto;

public class PolicyInquiryRequest {
    private String policyNo;
    private String requesterName;
    private String requesterPhone;
    private String requesterEmail;
    private String channel;

    public String getPolicyNo() { return policyNo; }
    public void setPolicyNo(String policyNo) { this.policyNo = policyNo; }
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    public String getRequesterPhone() { return requesterPhone; }
    public void setRequesterPhone(String requesterPhone) { this.requesterPhone = requesterPhone; }
    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}
