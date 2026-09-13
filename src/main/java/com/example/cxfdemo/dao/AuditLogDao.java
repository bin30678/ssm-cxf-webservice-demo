package com.example.cxfdemo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Repository
public class AuditLogDao {

    @Resource(name = "sqlSessionTemplate1")
    private SqlSessionTemplate sqlSessionTemplate;

    public void insertRequestLog(String guid, String ip, String clientType, String hostname, String uri, String method) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("guid", guid);
        param.put("ip", ip);
        param.put("clientType", clientType);
        param.put("hostname", hostname);
        param.put("uri", uri);
        param.put("method", method);
        try {
            sqlSessionTemplate.insert("AuditLog.insertRequestLog", param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insertResponseLog(String guid, Integer responseCode) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("guid", guid);
        param.put("responseCode", responseCode);
        try {
            sqlSessionTemplate.insert("AuditLog.insertResponseLog", param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insertFaultLog(String guid, String errorMsg) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("guid", guid);
        param.put("errorMsg", errorMsg);
        try {
            sqlSessionTemplate.insert("AuditLog.insertFaultLog", param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
