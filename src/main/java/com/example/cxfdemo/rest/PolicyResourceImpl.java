package com.example.cxfdemo.rest;

import com.example.cxfdemo.dto.PolicyInquiryRequest;
import com.example.cxfdemo.dto.PolicyInquiryResponse;
import com.example.cxfdemo.model.PolicyInfo;
import com.example.cxfdemo.service.AsyncBankTransferService;
import com.example.cxfdemo.service.MainProjectIntegrationService;
import com.example.cxfdemo.service.PolicyService;
import com.example.cxfdemo.utils.RestTemplateUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Service("policyResource")
public class PolicyResourceImpl implements PolicyResource {

    @Resource
    private PolicyService policyService;

    @Resource
    private AsyncBankTransferService asyncBankTransferService;

    @Resource
    private MainProjectIntegrationService mainProjectIntegrationService;

    @Resource
    private RestTemplateUtils restTemplateUtils;

    @Resource
    private com.example.cxfdemo.scheduler.ScheduledTasks scheduledTasks;

    public PolicyResourceImpl() {
    }

    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    @Override
    public Response getPolicies() {
        return Response.ok("{\"status\":\"ok\", \"message\":\"CXF REST API is working!\"}").build();
    }

    @Override
    public Response createPolicy(PolicyInfo policy) {
        if (policy == null) {
            policy = new PolicyInfo("P" + System.currentTimeMillis(), "新保戶", "意外險", "ACTIVE");
        } else if (policy.getPolicyNo() == null || policy.getPolicyNo().trim().isEmpty()) {
            policy.setPolicyNo("P" + System.currentTimeMillis());
        }
        
        try {
            policyService.createPolicy(policy);
        } catch (Exception e) {
            // 若主鍵衝突則更新
            policyService.updatePolicy(policy);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "CREATED");
        result.put("policy", policy);
        return Response.ok(result).build();
    }

    @Override
    public Response testAsyncBank() {
        String taskId = asyncBankTransferService.startAsyncTransferTask();
        return Response.ok("{\"message\":\"Async bank transfer started in background.\", \"taskId\":\"" + taskId + "\"}").build();
    }

    @Override
    public Response testExternalJar() {
        mainProjectIntegrationService.executeExternalLibraries();
        return Response.ok("{\"message\":\"External JAR simulated. Check console for DB2 connection passing!\"}").build();
    }

    @Override
    public Response testRestTemplate() {
        String result = restTemplateUtils.getWithLog("https://httpbin.org/get");
        return Response.ok("{\"message\":\"External API called and logged to DB!\", \"api_response_length\":" + result.length() + "}").build();
    }

    @Override
    public Response testCleanBackup(String dir, Integer days) {
        Map<String, Object> result = scheduledTasks.cleanOldBackupFiles(dir, days == null ? 7 : days);
        return Response.ok(result).build();
    }

    @Override
    public Response inquire(PolicyInquiryRequest request) {
        PolicyInfo info = policyService.find(request.getPolicyNo());
        if (info != null) {
            PolicyInquiryResponse response = new PolicyInquiryResponse(true, info, "API");
            return Response.ok(response).build();
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("status", "NOT_FOUND");
            error.put("message", "找無此保單");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
    }
}
