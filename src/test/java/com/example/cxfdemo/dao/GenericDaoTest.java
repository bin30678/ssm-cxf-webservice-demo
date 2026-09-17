package com.example.cxfdemo.dao;

import com.example.cxfdemo.utils.GenericDao;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

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
        GenericDao.close(null, null, null);
    }

    @Test
    public void testGetSystemProperty_withNullConnOrKey_shouldReturnNull() {
        Assert.assertNull(GenericDao.getSystemProperty((Connection) null, "test.key"));
        Assert.assertNull(GenericDao.getSystemProperty((String) null));
        Assert.assertNull(GenericDao.querySystemProperty((Connection) null, "test.key"));
        Assert.assertNull(GenericDao.querySystemProperty((String) null));
    }

    @Test
    public void testGetSystemProperty_withMockConnection_shouldReturnPropValue() throws SQLException {
        Connection mockConn = Mockito.mock(Connection.class);
        PreparedStatement mockPs = Mockito.mock(PreparedStatement.class);
        ResultSet mockRs = Mockito.mock(ResultSet.class);

        Mockito.when(mockConn.prepareStatement(Mockito.anyString())).thenReturn(mockPs);
        Mockito.when(mockPs.executeQuery()).thenReturn(mockRs);
        Mockito.when(mockRs.next()).thenReturn(true);
        Mockito.when(mockRs.getString("prop_key")).thenReturn("system.app.url");
        Mockito.when(mockRs.getString("prop_value")).thenReturn("http://localhost:8080/cxfdemo");

        String val = GenericDao.getSystemProperty(mockConn, "system.app.url");
        Assert.assertEquals("http://localhost:8080/cxfdemo", val);

        Map<String, String> map = GenericDao.querySystemProperty(mockConn, "system.app.url");
        Assert.assertNotNull(map);
        Assert.assertEquals("system.app.url", map.get("prop_key"));
        Assert.assertEquals("http://localhost:8080/cxfdemo", map.get("prop_value"));
    }
}
