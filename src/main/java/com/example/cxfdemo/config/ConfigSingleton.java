package com.example.cxfdemo.config;

import com.example.cxfdemo.utils.GenericDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系統參數快取單例類別 (ConfigSingleton)
 * 負責從 system_properties 資料庫表載入系統設定資訊。
 */
public class ConfigSingleton {

    private static final Logger log = LoggerFactory.getLogger(ConfigSingleton.class);

    private static volatile ConfigSingleton instance;

    private final Map<String, String> map = new ConcurrentHashMap<String, String>();

    private ConfigSingleton() {
    }

    /**
     * 取得單例實例，若需初始化 (needInit為true) 則自動執行 load()
     */
    public static ConfigSingleton getInstance() {
        if (instance == null) {
            synchronized (ConfigSingleton.class) {
                if (instance == null) {
                    instance = new ConfigSingleton();
                }
            }
        }
        if (instance.needInit()) {
            synchronized (instance) {
                if (instance.needInit()) {
                    instance.load();
                }
            }
        }
        return instance;
    }

    /**
     * 判斷 map 是否為空 (需要初始化)
     */
    public boolean needInit() {
        return map == null || map.isEmpty();
    }

    /**
     * 從 system_properties 讀取所有資料放進 map 裡
     */
    public synchronized void load() {
        log.info("ConfigSingleton.load() starting...");
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
                    String val = rs.getString("prop_value");
                    if (key != null) {
                        map.put(key, val != null ? val : "");
                        log.debug("Loaded ConfigSingleton property: {} = {}", key, val);
                    }
                }
                log.info("ConfigSingleton.load() completed, loaded {} properties.", map.size());
            } else {
                log.warn("ConfigSingleton.load() failed to obtain connection from GenericDao.getConnection1()");
            }
        } catch (Exception e) {
            log.error("Error loading properties in ConfigSingleton", e);
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (ps != null) try { ps.close(); } catch (Exception ignored) {}
            GenericDao.closeConnection(conn);
        }
    }

    /**
     * 清除 map
     */
    public synchronized void clear() {
        if (map != null) {
            map.clear();
            log.info("ConfigSingleton.clear() executed.");
        }
    }

    /**
     * 調用 clear，再調用 load
     */
    public synchronized void reload() {
        log.info("ConfigSingleton.reload() triggered.");
        clear();
        load();
    }

    /**
     * 取得 map 內部資料
     */
    public Map<String, String> getMap() {
        return map;
    }

    /**
     * 靜態便捷存取方法：傳 key 進去，取得資訊
     */
    public static String getMappingData(String key) {
        if (key == null) {
            return null;
        }
        return getInstance().getMap().get(key);
    }
}
