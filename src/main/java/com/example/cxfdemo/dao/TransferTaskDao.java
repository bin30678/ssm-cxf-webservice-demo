package com.example.cxfdemo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TransferTaskDao {

    private static final Logger log = LoggerFactory.getLogger(TransferTaskDao.class);

    @Resource(name = "sqlSessionTemplate1")
    private SqlSessionTemplate sqlSessionTemplate;

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public void insertMaster(String taskId, int status, String statusDesc) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("taskId", taskId);
        params.put("status", status);
        params.put("statusDesc", statusDesc);
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("TransferTask.insertMaster", params);
            }
        } catch (Exception e) {
            log.error("Failed to insertMaster for taskId: " + taskId, e);
        }
    }

    public void updateMasterStatus(String taskId, int status, String statusDesc) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("taskId", taskId);
        params.put("status", status);
        params.put("statusDesc", statusDesc);
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.update("TransferTask.updateMasterStatus", params);
            }
        } catch (Exception e) {
            log.error("Failed to updateMasterStatus for taskId: " + taskId, e);
        }
    }

    public void insertDetail(String taskId, int step, String message, boolean success) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("taskId", taskId);
        params.put("step", step);
        params.put("message", message);
        params.put("success", success ? 1 : 0);
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("TransferTask.insertDetail", params);
            }
        } catch (Exception e) {
            log.error("Failed to insertDetail for taskId: " + taskId, e);
        }
    }

    public Map<String, Object> selectMaster(String taskId) {
        try {
            if (sqlSessionTemplate != null) {
                return sqlSessionTemplate.selectOne("TransferTask.selectMaster", taskId);
            }
        } catch (Exception e) {
            log.error("Failed to selectMaster for taskId: " + taskId, e);
        }
        return null;
    }

    public List<Map<String, Object>> selectDetails(String taskId) {
        try {
            if (sqlSessionTemplate != null) {
                return sqlSessionTemplate.selectList("TransferTask.selectDetails", taskId);
            }
        } catch (Exception e) {
            log.error("Failed to selectDetails for taskId: " + taskId, e);
        }
        return null;
    }
}