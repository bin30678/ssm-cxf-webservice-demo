package com.example.cxfdemo.config;

import com.example.cxfdemo.utils.GenericDao;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import javax.sql.DataSource;
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
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("=== Loading Properties from Database ===");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            DataSource dataSource = (DataSource) beanFactory.getBean("dataSource1");
            conn = dataSource.getConnection();
            if (conn != null) {
                ps = conn.prepareStatement("SELECT prop_key, prop_value FROM system_properties");
                rs = ps.executeQuery();

                Properties dbProps = new Properties();
                while (rs.next()) {
                    String key = rs.getString("prop_key");
                    String value = rs.getString("prop_value");
                    dbProps.setProperty(key, value);
                    System.out.println("Loaded DB Property: " + key + " = " + value);
                }
                setProperties(dbProps);
            }
        } catch (Exception e) {
            System.err.println("Failed to load properties from database via beanFactory: " + e.getMessage());
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (ps != null) try { ps.close(); } catch (Exception ignored) {}
            GenericDao.closeConnection(conn);
        }

        super.postProcessBeanFactory(beanFactory);
    }
}
