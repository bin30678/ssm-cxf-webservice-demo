package com.example.cxfdemo.config;

/**
 * 全域靜態變數類別
 * 透過 Spring 的 MethodInvokingFactoryBean 來接收從 DB 查出來的 ${...} 設定
 */
public class GlobalConfig {

    private static String url;

    /**
     * 由 Spring MethodInvokingFactoryBean 呼叫的 Static Setter
     */
    public static void setUrl(String url) {
        GlobalConfig.url = url;
        System.out.println("=== [GlobalConfig] url is set to: " + url + " ===");
    }

    public static String getUrl() {
        return url;
    }
}
