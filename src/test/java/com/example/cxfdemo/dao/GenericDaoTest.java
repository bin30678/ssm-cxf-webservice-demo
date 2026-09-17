package com.example.cxfdemo.dao;

import com.example.cxfdemo.utils.GenericDao;
import org.junit.Assert;
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
    public void testGetSystemProperty_whenNoJndiOrNotFound_shouldReturnNull() {
        Assert.assertNull(GenericDao.getSystemProperty(null));
        Assert.assertNull(GenericDao.getSystemProperty("non.existent.key"));
    }

    @Test
    public void testGenericDaoConstructor() {
        GenericDao dao = new GenericDao();
        Assert.assertNotNull(dao);
    }
}
