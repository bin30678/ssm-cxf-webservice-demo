package com.example.cxfdemo.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * ?�用??GenericDao 工具類別
 * ?��? JNDI ?��? DataSource ??Connection，並?��??�本??CRUD ?��?工具?��?
 */
public class GenericDao {

    // JNDI ?�稱，�??��??��? Application Server 設�??��?修改
    // (例�? Tomcat ?�常?�綴??java:comp/env/jdbc/...)
    private static final String JNDI_NAME = "java:comp/env/jdbc/yourDataSourceName";

    private GenericDao() {
        // 私�?建�?子�??�止被實例�?
    }

    /**
     * ?��? JNDI ?��? DataSource 並�???Connection
     *
     * @return Connection
     * @throws SQLException 如�?資�?庫�??失�?
     * @throws NamingException 如�? JNDI ?��??��?�?     */
    public static Connection getConnection() throws SQLException, NamingException {
        Context initContext = new InitialContext();
        DataSource ds = (DataSource) initContext.lookup(JNDI_NAME);
        return ds.getConnection();
    }

    /**
     * 安全?��?資�?庫�??資�?
     */
    public static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ?�用：執�?CUD (Insert, Update, Delete) ?��?
     *
     * @param sql    SQL 語�? (?��????)
     * @param params 欲�?定�??�數
     * @return ?��??��??��???     */
    public static int executeUpdate(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        int rowsAffected = 0;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            setParameters(pstmt, params);
            rowsAffected = pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("資�?庫更?�錯�? " + e.getMessage(), e);
        } finally {
            close(conn, pstmt, null);
        }

        return rowsAffected;
    }

    /**
     * ?�用：執行查�?(Select) ?��?
     *
     * @param sql       SQL 語�? (?��????)
     * @param rowMapper 資�??��??�器
     * @param params    欲�?定�??�數
     * @return ?�詢結�??�表
     */
    public static <T> List<T> executeQuery(String sql, RowMapper<T> rowMapper, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<T> results = new ArrayList<T>();

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            setParameters(pstmt, params);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                results.add(rowMapper.mapRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("資�?庫查詢錯�? " + e.getMessage(), e);
        } finally {
            close(conn, pstmt, rs);
        }

        return results;
    }

    /**
     * 設�? PreparedStatement ?�數
     */
    private static void setParameters(PreparedStatement pstmt, Object... params) throws SQLException {
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]); // JDBC ?�數索�?�?1 ?��?
            }
        }
    }

    /**
     * ?�於?��? ResultSet 轉�??��?調�???(Callback)
     * 
     * @param <T> 轉�?後�??�件類�?
     */
    public interface RowMapper<T> {
        T mapRow(ResultSet rs) throws SQLException;
    }
}
