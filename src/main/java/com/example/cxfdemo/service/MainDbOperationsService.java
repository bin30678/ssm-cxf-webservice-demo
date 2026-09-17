package com.example.cxfdemo.service;

import com.example.cxfdemo.dao.PolicyDaoImpl;
import com.example.cxfdemo.model.PolicyInfo;

/**
 * SSM 主資料庫四種操作服務介面，與 Boot 專案保持完全等價之契約。
 * 支援 Mapper、SqlSessionTemplate、JdbcTemplate 與原生 JDBC，並透過 JDK 動態代理支援跨四種操作之原子交易控制。
 */
public interface MainDbOperationsService {

    PolicyInfo findViaMapper(String policyNo);

    void insertViaMapper(PolicyInfo policy);

    PolicyInfo findViaSqlSessionTemplate(String policyNo);

    void insertViaSqlSessionTemplate(PolicyInfo policy);

    PolicyInfo findViaJdbcTemplate(String policyNo);

    void insertViaJdbcTemplate(PolicyInfo policy);

    PolicyInfo findViaPureJdbc(String policyNo);

    void insertViaPureJdbc(PolicyInfo policy);

    PolicyDaoImpl getPolicyDao();

    void executeMixedFourOperations(
            PolicyInfo p1, PolicyInfo p2, PolicyInfo p3, PolicyInfo p4,
            boolean triggerRollback);
}
