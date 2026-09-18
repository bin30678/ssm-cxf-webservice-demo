package com.example.cxfdemo.dao;

import org.springframework.stereotype.Repository;

import com.example.cxfdemo.utils.GenericDao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SSM 示範 DB2 DAO。
 * 繼承 BaseDao，遵循公司最新架構：
 * 外部 DB2 資源透過 GenericDao.getConnection2() (jdbc/cxfdemo2) 取得，
 * 以 JDBC 方式執行查詢。
 */
@Repository
public class DemoImpDao extends BaseDao {

    public List<Map<String, Object>> queryUsingSqlSessionTemplate() {
        Connection conn = GenericDao.getConnection2();
        try {
            return queryUsingSqlSessionTemplate(conn);
        } finally {
            GenericDao.closeConnection(conn);
        }
    }

    public List<Map<String, Object>> queryUsingSqlSessionTemplate(Connection conn) {
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        if (conn == null) {
            return results;
        }
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement("SELECT * FROM mock_table_db2");
            rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<String, Object>();
                for (int i = 1; i <= colCount; i++) {
                    String colName = meta.getColumnName(i);
                    Object val = rs.getObject(i);
                    row.put(colName.toLowerCase(), val);
                    row.put(colName, val);
                }
                results.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
        }
        return results;
    }

    public List<Map<String, Object>> queryUsingJdbcTemplate() {
        Connection conn = GenericDao.getConnection2();
        try {
            return queryUsingSqlSessionTemplate(conn);
        } finally {
            GenericDao.closeConnection(conn);
        }
    }

    public List<String> queryUsingPureJdbc() {
        List<String> results = new ArrayList<String>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = GenericDao.getConnection2();
            if (conn != null) {
                ps = conn.prepareStatement("SELECT name FROM mock_table_db2");
                rs = ps.executeQuery();
                while (rs.next()) {
                    results.add(rs.getString("name"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            GenericDao.closeConnection(conn);
        }
        return results;
    }
}
