package com.example.cxfdemo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 主專案 BaseDao 抽象類別 (SSM 基準實作)。
 * 持有 SqlSessionTemplate、JdbcTemplate、DataSource 三個欄位。
 * 由 Spring 交易/資料存取配置注入，三個欄位均指向同一個主資料庫 (cxfdemo1)。
 */
public abstract class BaseDao {

    protected final SqlSessionTemplate sqlSessionTemplate;
    protected final JdbcTemplate jdbcTemplate;
    protected final DataSource dataSource;

    protected BaseDao(SqlSessionTemplate sqlSessionTemplate,
                      JdbcTemplate jdbcTemplate,
                      DataSource dataSource) {
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    public SqlSessionTemplate getSqlSessionTemplate() {
        return sqlSessionTemplate;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
