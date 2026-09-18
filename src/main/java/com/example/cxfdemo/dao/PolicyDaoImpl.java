package com.example.cxfdemo.dao;

import com.example.cxfdemo.model.PolicyInfo;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * SSM 業務 DaoImpl，繼承 BaseDao 並操作主專案 policy_info 表。
 * 建構子注入父類的三個欄位，均綁定主庫 cxfdemo1。
 */
@Repository("policyDao")
public class PolicyDaoImpl extends BaseDao implements PolicyDao {

    public PolicyDaoImpl() {
    }

    @Override
    public PolicyInfo findPolicyViaMapper(String policyNo) {
        return sqlSessionTemplate.selectOne("com.example.cxfdemo.mapper.DemoMapper.findPolicy", policyNo);
    }

    @Override
    public PolicyInfo findPolicyViaJdbc(String policyNo) {
        String sql = "SELECT policy_no, holder_name, product_name, status FROM policy_info WHERE policy_no = ?";
        List<PolicyInfo> list = jdbcTemplate.query(sql, new RowMapper<PolicyInfo>() {
            @Override
            public PolicyInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new PolicyInfo(
                        rs.getString("policy_no"),
                        rs.getString("holder_name"),
                        rs.getString("product_name"),
                        rs.getString("status")
                );
            }
        }, policyNo);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public PolicyInfo findPolicyViaPureJdbc(String policyNo) throws Exception {
        Connection conn = DataSourceUtils.getConnection(this.dataSource);
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT policy_no, holder_name, product_name, status FROM policy_info WHERE policy_no = ?");
            ps.setString(1, policyNo);
            ResultSet rs = ps.executeQuery();
            try {
                if (rs.next()) {
                    return new PolicyInfo(
                            rs.getString("policy_no"),
                            rs.getString("holder_name"),
                            rs.getString("product_name"),
                            rs.getString("status")
                    );
                }
            } finally {
                rs.close();
                ps.close();
            }
        } finally {
            DataSourceUtils.releaseConnection(conn, this.dataSource);
        }
        return null;
    }

    @Override
    public int insertPolicy(PolicyInfo policy) {
        String sql = "INSERT INTO policy_info (policy_no, holder_name, product_name, status) VALUES (?, ?, ?, ?)";
        return jdbcTemplate.update(sql, policy.getPolicyNo(), policy.getHolderName(), policy.getProductName(), policy.getStatus());
    }

    @Override
    public int updatePolicyStatus(String policyNo, String status) {
        String sql = "UPDATE policy_info SET status = ? WHERE policy_no = ?";
        return jdbcTemplate.update(sql, status, policyNo);
    }
}
