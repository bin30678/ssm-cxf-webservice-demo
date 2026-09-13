package com.example.cxfdemo.mapper;

import java.util.List;
import com.example.cxfdemo.model.FileUploadRecord;
import com.example.cxfdemo.model.PolicyInfo;

public interface DemoMapper {
    PolicyInfo findPolicy(String policyNo);
    List<PolicyInfo> findPolicies();
    int insertUpload(FileUploadRecord record);
    List<FileUploadRecord> findLatestUploads();
    int insertPolicy(PolicyInfo policy);
    int updatePolicy(PolicyInfo policy);
}
