package com.example.cxfdemo.dao;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.cxfdemo.utils.GenericDao;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * SSM 示範 DB2 DAO。
 * 遵循公司最新架構：
 * Spring 容器僅暴露單一主 DataSource Bean (dataSource1)。
 * 外部 DB2 資源透過 JNDI (jdbc/cxfdemo2) 取得，保留真實 SqlSessionTemplate、JdbcTemplate 與純 JDBC 存取行為。
 */
@Repository
public class DemoImpDao {

    private DataSource getDb2DataSource() {
        try {
            InitialContext ctx = new InitialContext();
            try {
                return (DataSource) ctx.lookup("java:comp/env/jdbc/cxfdemo2");
            } catch (Exception e) {
                return (DataSource) ctx.lookup("jdbc/cxfdemo2");
            }
        } catch (Exception e) {
            System.err.println("Failed to lookup DB2 DataSource via JNDI: " + e.getMessage());
            return null;
        }
    }

    private volatile SqlSessionTemplate db2SqlSessionTemplate;

    private synchronized SqlSessionTemplate getDb2SqlSessionTemplate() {
        if (db2SqlSessionTemplate == null) {
            DataSource ds = getDb2DataSource();
            if (ds != null) {
                try {
                    SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
                    factoryBean.setDataSource(ds);
                    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                    factoryBean.setMapperLocations(resolver.getResources("classpath*:mapper/DemoMapper2.xml"));
                    SqlSessionFactory sessionFactory = factoryBean.getObject();
                    if (sessionFactory != null) {
                        this.db2SqlSessionTemplate = new SqlSessionTemplate(sessionFactory);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to create SqlSessionTemplate for DB2 JNDI DataSource: " + e.getMessage());
                }
            }
        }
        return db2SqlSessionTemplate;
    }

    public List<Map<String, Object>> queryUsingSqlSessionTemplate() {
        SqlSessionTemplate template = getDb2SqlSessionTemplate();
        if (template != null) {
            return template.selectList("DemoMapper2.selectAll");
        }
        return Collections.emptyList();
    }

    public List<Map<String, Object>> queryUsingJdbcTemplate() {
        DataSource ds = getDb2DataSource();
        if (ds == null) {
            return Collections.emptyList();
        }
        String sql = "SELECT * FROM mock_table_db2";
        return new JdbcTemplate(ds).queryForList(sql);
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
