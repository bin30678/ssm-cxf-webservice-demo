package com.example.cxfdemo.config;

import com.example.cxfdemo.dao.GenericDao;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

/**
 * 客製化 PropertyPlaceholderConfigurer，在 Spring 啟動時先從 DB 載入設定。
 * 讓後續 Bean 均能使用 ${xxx} 取值，同時也將設定同步至 GlobalConfig。
 */
public class DatabasePropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    public DatabasePropertyPlaceholderConfigurer() {
        setIgnoreUnresolvablePlaceholders(true);
    }

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props) throws BeansException {
        
        System.out.println("=== Loading Properties from Database using GenericDao ===");
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = GenericDao.getConnection1();
            if (conn != null) {
                ps = conn.prepareStatement("SELECT prop_key, prop_value FROM system_properties");
                rs = ps.executeQuery();
                
                while (rs.next()) {
                    String key = rs.getString("prop_key");
                    String value = rs.getString("prop_value");
                    props.setProperty(key, value);
                    System.out.println("Loaded DB Property: " + key + " = " + value);
                }
            } else {
                System.err.println("Warning: Could not get Connection from GenericDao for properties!");
            }
        } catch (Exception e) {
            System.err.println("Failed to load properties from database");
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (ps != null) try { ps.close(); } catch (Exception e) {}
            GenericDao.closeConnection(conn);
        }

        super.processProperties(beanFactoryToProcess, props);
    }
}
