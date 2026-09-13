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
 * 通用型 GenericDao 工具類別
 * 透過 JNDI 取得 DataSource 與 Connection，並提供基本的 CRUD 封裝工具方法
 */
public class GenericDao {

    // JNDI 名稱，請根據您的 Application Server 設定進行修改
    // (例如 Tomcat 通常前綴為 java:comp/env/jdbc/...)
    private static final String JNDI_NAME = "java:comp/env/jdbc/yourDataSourceName";

    private GenericDao() {
        // 私有建構子，防止被實例化
    }

    /**
     * 透過 JNDI 尋找 DataSource 並取得 Connection
     *
     * @return Connection
     * @throws SQLException 如果資料庫連線失敗
     * @throws NamingException 如果 JNDI 查找失敗
     */
    public static Connection getConnection() throws SQLException, NamingException {
        Context initContext = new InitialContext();
        DataSource ds = (DataSource) initContext.lookup(JNDI_NAME);
        return ds.getConnection();
    }

    /**
     * 安全關閉資料庫連線資源
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
     * 通用：執行 CUD (Insert, Update, Delete) 操作
     *
     * @param sql    SQL 語句 (可帶 ?)
     * @param params 欲綁定的參數
     * @return 影響的資料筆數
     */
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
            throw new RuntimeException("資料庫更新錯誤: " + e.getMessage(), e);
        } finally {
            close(conn, pstmt, null);
        }

        return rowsAffected;
    }

    /**
     * 通用：執行查詢 (Select) 操作
     *
     * @param sql       SQL 語句 (可帶 ?)
     * @param rowMapper 資料映射器
     * @param params    欲綁定的參數
     * @return 查詢結果列表
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
            throw new RuntimeException("資料庫查詢錯誤: " + e.getMessage(), e);
        } finally {
            close(conn, pstmt, rs);
        }

        return results;
    }

    /**
     * 設定 PreparedStatement 參數
     */
    private static void setParameters(PreparedStatement pstmt, Object... params) throws SQLException {
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]); // JDBC 參數索引從 1 開始
            }
        }
    }

    /**
     * 用於將 ResultSet 轉換為物件的回調介面 (Callback)
     * 
     * @param <T> 轉換後的物件類型
     */
    public interface RowMapper<T> {
        T mapRow(ResultSet rs) throws SQLException;
    }
}
