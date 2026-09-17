package com.example.cxfdemo.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 通用資料庫連線與查詢工具類別 (GenericDao)
 * 模擬主專案與外部系統之多路資料庫連線：
 * - getConnection1(): cxfdemo1 主資料庫連線 (模擬主專案 DB 連線)
 * - getConnection2(): cxfdemo2 資料庫連線 (模擬 Oracle 連線)
 * - getConnection3(): AS400 系統 A 連線 (模擬 AS400 連線)
 * - getConnection4(): AS400 系統 B 連線 (模擬 AS400 連線)
 */
public class GenericDao {

    private static final Logger log = LoggerFactory.getLogger(GenericDao.class);

    public GenericDao() {
    }

    /** 路徑 1: 主資料庫 (cxfdemo1) - 模擬主專案的 DB 連線 */
    public static Connection getConnection1() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/cxfdemo1");
            return ds.getConnection();
        } catch (Exception e) {
            log.error("Failed to get connection1 from java:comp/env/jdbc/cxfdemo1", e);
            return null;
        }
    }

    /** 路徑 2: 模擬 Oracle 連線 (cxfdemo2) */
    public static Connection getConnection2() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/cxfdemo2");
            return ds.getConnection();
        } catch (Exception e) {
            log.error("Failed to get connection2 from java:comp/env/jdbc/cxfdemo2", e);
            return null;
        }
    }

    /** 路徑 3: 模擬 AS400 系統 A 連線 (as400_a) */
    public static Connection getConnection3() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/as400_a");
            return ds.getConnection();
        } catch (Exception e) {
            log.error("Failed to get connection3 from java:comp/env/jdbc/as400_a", e);
            return null;
        }
    }

    /** 路徑 4: 模擬 AS400 系統 B 連線 (as400_b) */
    public static Connection getConnection4() {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/as400_b");
            return ds.getConnection();
        } catch (Exception e) {
            log.error("Failed to get connection4 from java:comp/env/jdbc/as400_b", e);
            return null;
        }
    }

    /**
     * 查詢系統參數：SELECT prop_key, prop_value FROM system_properties WHERE prop_key = ?
     * 透過 getConnection1() 取得 cxfdemo1 主專案連線查詢
     *
     * @param propKey 欲查詢之參數名稱鍵值
     * @return 查得之 prop_value，查無資料或異常時回傳 null
     */
    public static String getSystemProperty(String propKey) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection1();
            if (conn != null) {
                ps = conn.prepareStatement("SELECT prop_key, prop_value FROM system_properties WHERE prop_key = ?");
                ps.setString(1, propKey);
                rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString("prop_value");
                }
            }
        } catch (Exception e) {
            log.error("Failed to query system_properties for prop_key: " + propKey, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            closeConnection(conn);
        }
        return null;
    }

    /**
     * 安全關閉資料庫連線
     */
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
