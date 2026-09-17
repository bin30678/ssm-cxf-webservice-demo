package com.example.cxfdemo.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

    // 四個 JNDI 名稱常數（完整名稱與相對名稱）
    public static final String JNDI_CXFDEMO1 = "java:comp/env/jdbc/cxfdemo1";
    public static final String JNDI_CXFDEMO2 = "java:comp/env/jdbc/cxfdemo2";
    public static final String JNDI_AS400_A = "java:comp/env/jdbc/as400_a";
    public static final String JNDI_AS400_B = "java:comp/env/jdbc/as400_b";

    public static final String JNDI_CXFDEMO1_SHORT = "jdbc/cxfdemo1";
    public static final String JNDI_CXFDEMO2_SHORT = "jdbc/cxfdemo2";
    public static final String JNDI_AS400_A_SHORT = "jdbc/as400_a";
    public static final String JNDI_AS400_B_SHORT = "jdbc/as400_b";

    public GenericDao() {
    }

    /** 路徑 1: 主資料庫 (cxfdemo1) - 模擬主專案的 DB 連線 */
    public static Connection getConnection1() {
        return getConnectionByJndi(JNDI_CXFDEMO1);
    }

    /** 路徑 2: 模擬 Oracle 連線 (cxfdemo2) */
    public static Connection getConnection2() {
        return getConnectionByJndi(JNDI_CXFDEMO2);
    }

    /** 路徑 3: 模擬 AS400 系統 A 連線 (as400_a) */
    public static Connection getConnection3() {
        return getConnectionByJndi(JNDI_AS400_A);
    }

    /** 路徑 4: 模擬 AS400 系統 B 連線 (as400_b) */
    public static Connection getConnection4() {
        return getConnectionByJndi(JNDI_AS400_B);
    }

    /** 別名：相容原有 getConnection() -> getConnection1() */
    public static Connection getConnection() {
        return getConnection1();
    }

    /**
     * 依 JNDI 名稱取得 Connection。
     * 自動支援 java:comp/env/ 前綴與相對路徑互轉容錯。
     */
    public static Connection getConnectionByJndi(String jndiName) {
        if (jndiName == null || jndiName.trim().isEmpty()) {
            log.warn("JNDI name is null or empty");
            return null;
        }
        Connection conn = null;
        try {
            Context ctx = new InitialContext();
            DataSource ds = lookupDataSource(ctx, jndiName);
            if (ds != null) {
                conn = ds.getConnection();
            }
        } catch (Exception e) {
            log.error("Failed to get connection from JNDI: " + jndiName, e);
        }
        return conn;
    }

    /**
     * 查找 JNDI DataSource 物件
     */
    public static DataSource lookupDataSource(Context ctx, String jndiName) {
        try {
            return (DataSource) ctx.lookup(jndiName);
        } catch (NamingException e1) {
            try {
                if (jndiName.startsWith("java:comp/env/")) {
                    String shortName = jndiName.substring("java:comp/env/".length());
                    return (DataSource) ctx.lookup(shortName);
                } else {
                    String fullName = "java:comp/env/" + jndiName;
                    return (DataSource) ctx.lookup(fullName);
                }
            } catch (NamingException e2) {
                log.warn("DataSource JNDI lookup failed for both '" + jndiName + "' and alternative: " + e2.getMessage());
                return null;
            }
        }
    }

    /**
     * 查詢系統參數：SELECT prop_key, prop_value FROM system_properties WHERE prop_key = ?
     * 預設透過 getConnection1()（主資料庫 cxfdemo1）取得連線進行查詢。
     *
     * @param propKey 欲查詢之參數名稱鍵值
     * @return 查得之 prop_value，查無資料或異常時回傳 null
     */
    public static String getSystemProperty(String propKey) {
        Connection conn = null;
        try {
            conn = getConnection1();
            return getSystemProperty(conn, propKey);
        } finally {
            closeConnection(conn);
        }
    }

    /**
     * 查詢系統參數：SELECT prop_key, prop_value FROM system_properties WHERE prop_key = ?
     * 允許傳入既有連線，查詢完畢不關閉傳入之連線。
     *
     * @param conn 資料庫連線 (建議為主庫 cxfdemo1 連線)
     * @param propKey 欲查詢之參數名稱鍵值
     * @return 查得之 prop_value，查無資料或異常時回傳 null
     */
    public static String getSystemProperty(Connection conn, String propKey) {
        if (conn == null || propKey == null) {
            return null;
        }
        String sql = "SELECT prop_key, prop_value FROM system_properties WHERE prop_key = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, propKey);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("prop_value");
            }
        } catch (SQLException e) {
            log.error("Failed to query system_properties for prop_key: " + propKey, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
        }
        return null;
    }

    /**
     * 查詢系統參數鍵值對：SELECT prop_key, prop_value FROM system_properties WHERE prop_key = ?
     *
     * @param propKey 欲查詢之參數名稱鍵值
     * @return 包含 prop_key 與 prop_value 的 Map，查無資料回傳 null
     */
    public static Map<String, String> querySystemProperty(String propKey) {
        Connection conn = null;
        try {
            conn = getConnection1();
            return querySystemProperty(conn, propKey);
        } finally {
            closeConnection(conn);
        }
    }

    /**
     * 查詢系統參數鍵值對：SELECT prop_key, prop_value FROM system_properties WHERE prop_key = ?
     *
     * @param conn 資料庫連線
     * @param propKey 欲查詢之參數名稱鍵值
     * @return 包含 prop_key 與 prop_value 的 Map，查無資料回傳 null
     */
    public static Map<String, String> querySystemProperty(Connection conn, String propKey) {
        if (conn == null || propKey == null) {
            return null;
        }
        String sql = "SELECT prop_key, prop_value FROM system_properties WHERE prop_key = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, propKey);
            rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, String> map = new HashMap<String, String>();
                map.put("prop_key", rs.getString("prop_key"));
                map.put("prop_value", rs.getString("prop_value"));
                return map;
            }
        } catch (SQLException e) {
            log.error("Failed to query system_properties map for prop_key: " + propKey, e);
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
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

    /**
     * 安全關閉連線與查詢資源
     */
    public static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (Exception e) {}
        if (pstmt != null) try { pstmt.close(); } catch (Exception e) {}
        closeConnection(conn);
    }
}
