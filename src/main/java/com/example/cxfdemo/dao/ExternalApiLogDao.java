package com.example.cxfdemo.dao;

import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Repository
public class ExternalApiLogDao extends BaseDao {

    public void insertApiLog(String url, String requestParam, String responseBody, String exceptionMsg, Date callTime) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("url", url);
        param.put("requestParam", requestParam);
        param.put("responseBody", responseBody);
        param.put("exceptionMsg", exceptionMsg);
        param.put("callTime", callTime);
        
        try {
            if (sqlSessionTemplate != null) {
                sqlSessionTemplate.insert("ExternalApiLog.insertApiLog", param);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
