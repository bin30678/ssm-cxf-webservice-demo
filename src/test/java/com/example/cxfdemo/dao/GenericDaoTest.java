package com.example.cxfdemo.dao;

import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.SQLException;

public class GenericDaoTest {

    @Test
    public void testCloseConnection_withNull_shouldNotThrow() {
        GenericDao.closeConnection(null);
    }

    @Test
    public void testCloseConnection_withMockConnection_shouldClose() throws SQLException {
        Connection mockConn = Mockito.mock(Connection.class);
        GenericDao.closeConnection(mockConn);
        Mockito.verify(mockConn, Mockito.times(1)).close();
    }

    @Test
    public void testCloseConnection_whenThrowsSqlException_shouldHandleGracefully() throws SQLException {
        Connection mockConn = Mockito.mock(Connection.class);
        Mockito.doThrow(new SQLException("DB error on close")).when(mockConn).close();
        GenericDao.closeConnection(mockConn);
    }

    @Test
    public void testUtilsGenericDaoClose_withNull_shouldNotThrow() {
        com.example.cxfdemo.utils.GenericDao.close(null, null, null);
    }
}
