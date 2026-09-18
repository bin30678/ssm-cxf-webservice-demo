package com.example.cxfdemo.service;

import com.example.cxfdemo.dao.PolicyDaoImpl;
import com.example.cxfdemo.mapper.DemoMapper;
import com.example.cxfdemo.model.PolicyInfo;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * SSM 主資料庫四種操作服務實作類別，以 JDK 動態代理支援 Spring @Transactional 交易控制。
 */
@Service("mainDbOperationsService")
public class MainDbOperationsServiceImpl implements MainDbOperationsService {

    private static final Logger log = LoggerFactory.getLogger(MainDbOperationsServiceImpl.class);

    private final DemoMapper demoMapper;
    private final SqlSessionTemplate sqlSessionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final PolicyDaoImpl policyDao;

    @Autowired
    public MainDbOperationsServiceImpl(
            DemoMapper demoMapper,
            @Qualifier("sqlSessionTemplate1") SqlSessionTemplate sqlSessionTemplate,
            @Qualifier("jdbcTemplate1") JdbcTemplate jdbcTemplate,
            @Qualifier("dataSource1") DataSource dataSource,
            PolicyDaoImpl policyDao) {
        this.demoMapper = demoMapper;
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.policyDao = policyDao;
    }

    @Override
    public PolicyInfo findViaMapper(String policyNo) {
        log.info("MainDbOperationsServiceImpl.findViaMapper called for policyNo: {}", policyNo);
        return demoMapper.findPolicy(policyNo);
    }

    @Override
    public void insertViaMapper(PolicyInfo policy) {
        log.info("MainDbOperationsServiceImpl.insertViaMapper called for policy: {}", policy);
        demoMapper.insertPolicy(policy);
    }

    @Override
    public PolicyInfo findViaSqlSessionTemplate(String policyNo) {
        log.info("MainDbOperationsServiceImpl.findViaSqlSessionTemplate called for policyNo: {}", policyNo);
        return sqlSessionTemplate.selectOne(
                "com.example.cxfdemo.mapper.DemoMapper.findPolicy", policyNo);
    }

    @Override
    public void insertViaSqlSessionTemplate(PolicyInfo policy) {
        log.info("MainDbOperationsServiceImpl.insertViaSqlSessionTemplate called for policy: {}", policy);
        sqlSessionTemplate.insert(
                "com.example.cxfdemo.mapper.DemoMapper.insertPolicy", policy);
    }

    @Override
    public PolicyInfo findViaJdbcTemplate(String policyNo) {
        log.info("MainDbOperationsServiceImpl.findViaJdbcTemplate called for policyNo: {}", policyNo);
        return policyDao.findPolicyViaJdbc(policyNo);
    }

    @Override
    public void insertViaJdbcTemplate(PolicyInfo policy) {
        log.info("MainDbOperationsServiceImpl.insertViaJdbcTemplate called for policy: {}", policy);
        policyDao.insertPolicy(policy);
    }

    @Override
    public PolicyInfo findViaPureJdbc(String policyNo) {
        log.info("MainDbOperationsServiceImpl.findViaPureJdbc called for policyNo: {}", policyNo);
        try {
            return policyDao.findPolicyViaPureJdbc(policyNo);
        } catch (Exception e) {
            log.error("Query via pure JDBC failed for policyNo: {}", policyNo, e);
            throw new RuntimeException("Query via pure JDBC failed", e);
        }
    }

    @Override
    public void insertViaPureJdbc(PolicyInfo policy) {
        log.info("MainDbOperationsServiceImpl.insertViaPureJdbc called for policy: {}", policy);
        Connection conn = DataSourceUtils.getConnection(dataSource);
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement("INSERT INTO policy_info (policy_no, holder_name, product_name, status) VALUES (?, ?, ?, ?)");
            ps.setString(1, policy.getPolicyNo());
            ps.setString(2, policy.getHolderName());
            ps.setString(3, policy.getProductName());
            ps.setString(4, policy.getStatus());
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("Insert via pure JDBC failed for policy: {}", policy, e);
            throw new RuntimeException("Insert via pure JDBC failed", e);
        } finally {
            if (ps != null) try { ps.close(); } catch (Exception ignored) {}
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @Override
    public PolicyDaoImpl getPolicyDao() {
        return policyDao;
    }

    @Override
    @Transactional(value = "transactionManager1")
    public void executeMixedFourOperations(
            PolicyInfo p1, PolicyInfo p2, PolicyInfo p3, PolicyInfo p4,
            boolean triggerRollback) {
        log.info("MainDbOperationsServiceImpl.executeMixedFourOperations called, triggerRollback: {}", triggerRollback);
        insertViaMapper(p1);
        insertViaSqlSessionTemplate(p2);
        insertViaJdbcTemplate(p3);
        insertViaPureJdbc(p4);

        if (triggerRollback) {
            log.warn("MainDbOperationsServiceImpl.executeMixedFourOperations throwing simulated rollback exception");
            throw new RuntimeException("Simulated transaction rollback exception");
        }
    }
}
