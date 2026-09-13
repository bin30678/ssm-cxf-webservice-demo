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
        // ?�設??JNDI ?��??��??????null，�??��??��?�?        service.executeExternalLibraries(null);
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

        // 驗�?交�?管�??�主專�??�管
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

        // 驗�??��??�常?�主專�?負責 rollback �?close
        Mockito.verify(mockConn, Mockito.times(1)).rollback();
        Mockito.verify(mockConn, Mockito.times(1)).close();
    }
}
