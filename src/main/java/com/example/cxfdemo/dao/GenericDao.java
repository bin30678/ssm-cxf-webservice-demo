package com.example.cxfdemo.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 傳統使用 InitialContext 進行 lookup DataSource 並取得 Connection 的 Utils
 * 例如: GenericDao.getConnection1()
 */
public class GenericDao {

    private static final Logger log = LoggerFactory.getLogger(GenericDao.class);

    private GenericDao() {
        // 私有建構子，防止被 new
    }

    public static Connection getConnection1() {
        return getConnectionByJndi("java:comp/env/jdbc/cxfdemo1");
    }

    public static Connection getConnection2() {
        return getConnectionByJndi("java:comp/env/jdbc/cxfdemo2");
    }

    private static Connection getConnectionByJndi(String jndiName) {
        Connection conn = null;
        try {
            Context initContext = new InitialContext();
            DataSource ds = (DataSource) initContext.lookup(jndiName);
            if (ds != null) {
                conn = ds.getConnection();
            }
        } catch (Exception e) {
            log.error("Failed to get connection from JNDI: " + jndiName, e);
        }
        return conn;
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("Failed to close connection", e);
            }
        }
    }
}
