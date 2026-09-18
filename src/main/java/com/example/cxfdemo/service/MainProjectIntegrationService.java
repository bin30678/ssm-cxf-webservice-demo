package com.example.cxfdemo.service;

import com.example.cxfdemo.utils.GenericDao;
import com.external.liba.service.LegacyAS400Service;
import com.external.libb.dao.AnotherLegacyDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Connection;
import java.util.List;

@Service
public class MainProjectIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(MainProjectIntegrationService.class);

    // 外部 JAR A：有 Service + DAO 層，注入它的 Service
    @Resource(name = "legacyAS400Service")
    private LegacyAS400Service libAService;

    // 外部 JAR B：僅有 DAO，無 Service 層，直接注入 DAO
    @Resource(name = "anotherLegacyDao")
    private AnotherLegacyDao libBDao;

    public void setLibAService(LegacyAS400Service libAService) {
        this.libAService = libAService;
    }

    public void setLibBDao(AnotherLegacyDao libBDao) {
        this.libBDao = libBDao;
    }

    /**
     * 示範如何由主專案控管 Connection，並呼叫多個外部 JAR
     */
    public void executeExternalLibraries() {
        executeExternalLibraries(GenericDao.getConnection2());
    }

    /**
     * 多載方法，方便傳入 Connection 進行測試或特定環境使用
     */
    public void executeExternalLibraries(Connection conn) {
        if (conn == null) {
            log.warn("Failed to get AS400 Connection (Connection is null)");
            return;
        }

        try {
            conn.setAutoCommit(false);

            // 1. 呼叫外部 JAR A (有 Service 層，調用 Service 方法)
            if (libAService != null) {
                List<String> activeCustomers = libAService.getActiveCustomers(conn);
                log.info("Active customers from Lib A (Service+DAO): {}", activeCustomers);
            }

            // 2. 呼叫外部 JAR B (僅 DAO，主專案直接呼叫 DAO 方法)
            if (libBDao != null) {
                int inactiveCount = libBDao.queryInactiveCustomerCount(conn);
                log.info("Inactive customer count from Lib B (DAO only): {}", inactiveCount);
            }

            conn.commit();

        } catch (Exception e) {
            log.error("Error executing external libraries", e);
            try {
                if (conn != null) conn.rollback();
            } catch (Exception re) {
                log.error("Error rolling back connection", re);
            }
        } finally {
            GenericDao.closeConnection(conn);
            log.info("=== Connection completely closed by Main Project ===");
        }
    }
}
