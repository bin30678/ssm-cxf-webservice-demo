package com.example.cxfdemo.service;

import com.external.liba.service.LegacyAS400Service;
import com.external.libb.dao.AnotherLegacyDao;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.util.Collections;

public class MainProjectIntegrationServiceTest {

    @Test
    public void testExecuteExternalLibraries_withNullConnection_shouldReturnSafely() {
        MainProjectIntegrationService service = new MainProjectIntegrationService();
        // 預設若 JNDI 查找失敗取得 null，應安全返回
        service.executeExternalLibraries(null);
    }

    @Test
    public void testExecuteExternalLibraries_successFlow() throws Exception {
        MainProjectIntegrationService service = new MainProjectIntegrationService();

        LegacyAS400Service mockLibAService = Mockito.mock(LegacyAS400Service.class);
        AnotherLegacyDao mockLibBDao = Mockito.mock(AnotherLegacyDao.class);
        Connection mockConn = Mockito.mock(Connection.class);

        service.setLibAService(mockLibAService);
        service.setLibBDao(mockLibBDao);

        Mockito.when(mockLibAService.getActiveCustomers(mockConn))
                .thenReturn(Collections.singletonList("CustA"));
        Mockito.when(mockLibBDao.queryInactiveCustomerCount(mockConn))
                .thenReturn(5);

        service.executeExternalLibraries(mockConn);

        // 驗證交易管理由主專案控管
        Mockito.verify(mockConn, Mockito.times(1)).setAutoCommit(false);
        Mockito.verify(mockLibAService, Mockito.times(1)).getActiveCustomers(mockConn);
        Mockito.verify(mockLibBDao, Mockito.times(1)).queryInactiveCustomerCount(mockConn);
        Mockito.verify(mockConn, Mockito.times(1)).commit();
        Mockito.verify(mockConn, Mockito.times(1)).close();
    }

    @Test
    public void testExecuteExternalLibraries_failureFlow_shouldRollbackAndClose() throws Exception {
        MainProjectIntegrationService service = new MainProjectIntegrationService();

        LegacyAS400Service mockLibAService = Mockito.mock(LegacyAS400Service.class);
        AnotherLegacyDao mockLibBDao = Mockito.mock(AnotherLegacyDao.class);
        Connection mockConn = Mockito.mock(Connection.class);

        service.setLibAService(mockLibAService);
        service.setLibBDao(mockLibBDao);

        Mockito.when(mockLibAService.getActiveCustomers(mockConn))
                .thenThrow(new RuntimeException("AS400 communication error"));

        service.executeExternalLibraries(mockConn);

        // 驗證拋出異常由主專案負責 rollback 與 close
        Mockito.verify(mockConn, Mockito.times(1)).rollback();
        Mockito.verify(mockConn, Mockito.times(1)).close();
    }
}
