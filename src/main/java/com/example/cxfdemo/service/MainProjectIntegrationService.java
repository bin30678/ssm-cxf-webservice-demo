package com.example.cxfdemo.service;

import com.example.cxfdemo.dao.GenericDao;
import com.external.liba.service.LegacyAS400Service;
import com.external.libb.dao.AnotherLegacyDao;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Connection;
import java.util.List;

@Service
public class MainProjectIntegrationService {

    // 外部 JAR A：�? Service + DAO 層�?注入它�? Service
    @Resource(name = "legacyAS400Service")
    private LegacyAS400Service libAService;

    // 外部 JAR B：�? DAO，�???Service 層�??�接注入 DAO
    @Resource(name = "anotherLegacyDao")
    private AnotherLegacyDao libBDao;

    public void setLibAService(LegacyAS400Service libAService) {
        this.libAService = libAService;
    }

    public void setLibBDao(AnotherLegacyDao libBDao) {
        this.libBDao = libBDao;
    }

    /**
     * 示�?如�??�主專�??�管 Connection，並?�叫多個�???JAR
     */
    public void executeExternalLibraries() {
        executeExternalLibraries(GenericDao.getConnection2());
    }

    /**
     * ?��??��??��?，方便傳??Connection ?��?測試?�特定�?境使??     */
    public void executeExternalLibraries(Connection conn) {
        if (conn == null) {
            System.out.println("Failed to get AS400 Connection (Connection is null)");
            return;
        }

        try {
            conn.setAutoCommit(false);

            // 1. ?�叫外部 JAR A (??Service 層�??��? Service ?��?)
            if (libAService != null) {
                List<String> activeCustomers = libAService.getActiveCustomers(conn);
                System.out.println("Active customers from Lib A (Service+DAO): " + activeCustomers);
            }

            // 2. ?�叫外部 JAR B (�?DAO，主專�??�接?�叫 DAO ?��?)
            if (libBDao != null) {
                int inactiveCount = libBDao.queryInactiveCustomerCount(conn);
                System.out.println("Inactive customer count from Lib B (DAO only): " + inactiveCount);
            }

            conn.commit();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (Exception re) {
                re.printStackTrace();
            }
        } finally {
            GenericDao.closeConnection(conn);
            System.out.println("=== Connection completely closed by Main Project ===");
        }
    }
}
