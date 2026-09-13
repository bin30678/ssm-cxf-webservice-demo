package com.example.cxfdemo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class AuditLogDao {

    private static final Logger log = LoggerFactory.getLogger(AuditLogDao.class);

    @Resource(name = "sqlSessionTemplate1")
    private SqlSessionTemplate sqlSessionTemplate;

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public void insertRequestLog(String guid, String ip, String clientType, String hostname, String uri, String method) {
        insertRequestLog(guid, ip, clientType, hostname, uri, method, null);
    }

    public void insertRequestLog(String guid, String ip, String clientType, String hostname, String uri, String method, String payload) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("guid", guid);
        param.put("ip", ip);
        param.put("clientType", clientType);
        param.put("hostname", hostname);
        param.put("uri", uri);
        param.put("method", method);
        param.put("payload", payload);
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("AuditLog.insertRequestLog", param);
            } else {
                log.error("sqlSessionTemplate is null in AuditLogDao!");
            }
        } catch (Exception e) {
            log.error("Failed to insertRequestLog for guid: " + guid, e);
        }
    }

    public void insertResponseLog(String guid, Integer responseCode) {
        insertResponseLog(guid, responseCode, null);
    }

    public void insertResponseLog(String guid, Integer responseCode, String payload) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("guid", guid);
        param.put("responseCode", responseCode);
        param.put("payload", payload);
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("AuditLog.insertResponseLog", param);
            } else {
                log.error("sqlSessionTemplate is null in AuditLogDao!");
            }
        } catch (Exception e) {
            log.error("Failed to insertResponseLog for guid: " + guid, e);
        }
    }

    public void insertFaultLog(String guid, String errorMsg) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("guid", guid);
        param.put("errorMsg", errorMsg);
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("AuditLog.insertFaultLog", param);
            } else {
                log.error("sqlSessionTemplate is null in AuditLogDao!");
            }
        } catch (Exception e) {
            log.error("Failed to insertFaultLog for guid: " + guid, e);
        }
    }
}
