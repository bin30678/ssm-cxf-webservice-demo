package com.example.cxfdemo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class DemoImpDao {

    // 2. SqlSessionTemplate 方式
    @Resource(name = "sqlSessionTemplate2")
    private SqlSessionTemplate sqlSessionTemplate2;

    // 3. JdbcTemplate 方式
    @Resource(name = "jdbcTemplate2")
    private JdbcTemplate jdbcTemplate2;

    // 4. Pure JDBC 方式 (直接取得 DataSource 的 Connection)
    @Resource(name = "dataSource2")
    private DataSource dataSource2;

    public List<Map<String, Object>> queryUsingSqlSessionTemplate() {
        // 對應 mapper xml 中的 namespace.id = DemoMapper2.selectAll
        return sqlSessionTemplate2.selectList("DemoMapper2.selectAll");
    }

    public List<Map<String, Object>> queryUsingJdbcTemplate() {
        String sql = "SELECT * FROM mock_table_db2";
        return jdbcTemplate2.queryForList(sql);
    }

    public List<String> queryUsingPureJdbc() {
        List<String> results = new ArrayList<String>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dataSource2.getConnection();
            ps = conn.prepareStatement("SELECT name FROM mock_table_db2");
            rs = ps.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
        return results;
    }
}
