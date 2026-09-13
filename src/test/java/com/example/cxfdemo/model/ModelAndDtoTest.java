package com.example.cxfdemo.model;

import com.example.cxfdemo.dto.PolicyInquiryRequest;
import com.example.cxfdemo.dto.PolicyInquiryResponse;
import com.example.cxfdemo.fault.ApiError;
import com.example.cxfdemo.fault.ServiceFaultException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

public class ModelAndDtoTest {

    @Test
    public void testPolicyInfo() {
        PolicyInfo info = new PolicyInfo();
        info.setPolicyNo("POL001");
        info.setHolderName("王大明");
        info.setStatus("ACTIVE");

        Assert.assertEquals("POL001", info.getPolicyNo());
        Assert.assertEquals("王大明", info.getHolderName());
        Assert.assertEquals("ACTIVE", info.getStatus());
    }

    @Test
    public void testFileUploadRecord() {
        FileUploadRecord record = new FileUploadRecord();
        Date now = new Date();

        record.setId(10L);
        record.setOriginalName("test.pdf");
        record.setStoredName("uuid-test.pdf");
        record.setContentType("application/pdf");
        record.setSizeBytes(1024L);
        record.setDescription("保單條款");
        record.setCreatedAt(now);

        Assert.assertEquals(Long.valueOf(10L), record.getId());
        Assert.assertEquals("test.pdf", record.getOriginalName());
        Assert.assertEquals("uuid-test.pdf", record.getStoredName());
        Assert.assertEquals("application/pdf", record.getContentType());
        Assert.assertEquals(1024L, (long) record.getSizeBytes());
        Assert.assertEquals("保單條款", record.getDescription());
        Assert.assertEquals(now, record.getCreatedAt());
    }

    @Test
    public void testPolicyInquiryDto() {
        PolicyInquiryRequest req = new PolicyInquiryRequest();
        req.setPolicyNo("REQ_001");
        Assert.assertEquals("REQ_001", req.getPolicyNo());

        PolicyInfo policy = new PolicyInfo();
        policy.setPolicyNo("P001");
        PolicyInquiryResponse resp = new PolicyInquiryResponse(true, policy, "WEB");
        Assert.assertTrue(resp.isSuccess());
        Assert.assertEquals("P001", resp.getPolicy().getPolicyNo());
        Assert.assertEquals("WEB", resp.getChannel());
    }

    @Test
    public void testApiErrorAndServiceFaultException() {
        ApiError error = new ApiError("ERR_CODE_01", "測試錯誤訊息", "trace-xyz-123");
        Assert.assertEquals("ERR_CODE_01", error.getCode());
        Assert.assertEquals("測試錯誤訊息", error.getMessage());
        Assert.assertEquals("trace-xyz-123", error.getTraceId());

        ServiceFaultException fault = new ServiceFaultException("NOT_FOUND", "找無此保單", 404);
        Assert.assertEquals("NOT_FOUND", fault.getCode());
        Assert.assertEquals("找無此保單", fault.getMessage());
        Assert.assertEquals(404, fault.getHttpStatus());
    }
}
