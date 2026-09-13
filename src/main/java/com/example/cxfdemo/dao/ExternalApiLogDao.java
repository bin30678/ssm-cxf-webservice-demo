package com.example.cxfdemo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Repository
public class ExternalApiLogDao {

    @Resource(name = "sqlSessionTemplate1")
    private SqlSessionTemplate sqlSessionTemplate;

    public void insertApiLog(String url, String requestParam, String responseBody, String exceptionMsg, Date callTime) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("url", url);
        param.put("requestParam", requestParam);
        param.put("responseBody", responseBody);
        param.put("exceptionMsg", exceptionMsg);
        param.put("callTime", callTime);
        
        try {
            sqlSessionTemplate.insert("ExternalApiLog.insertApiLog", param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
