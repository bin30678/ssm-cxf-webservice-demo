package com.example.cxfdemo.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 傳統使用 InitialContext 進行 lookup DataSource 並取得 Connection 的 Utils
 * 例如: GenericDao.getConnection1()
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

    /** 路徑 1: 主資料庫 (cxfdemo1) */
    public static Connection getConnection1() {
        return getConnectionByJndi(JNDI_CXFDEMO1);
    }

    /** 路徑 2: 外部專案資料庫 (cxfdemo2) */
    public static Connection getConnection2() {
        return getConnectionByJndi(JNDI_CXFDEMO2);
    }

    /** 路徑 3: 外部 AS400 系統 A */
    public static Connection getConnection3() {
        return getConnectionByJndi(JNDI_AS400_A);
    }

    /** 路徑 4: 外部 AS400 系統 B */
    public static Connection getConnection4() {
        return getConnectionByJndi(JNDI_AS400_B);
    }

    /** 別名：相容原有 getConnection() -> getConnection1() */
    public Connection getConnection() {
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
