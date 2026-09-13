package com.example.cxfdemo.config;

/**
 * ?��??��??�數類別
 * ?��? Spring ??MethodInvokingFactoryBean 來�???DB ?�出來�? ${...} 設�?
 */
public class GlobalConfig {

    private static String url;

    /**
     * �?Spring MethodInvokingFactoryBean ?�叫??Static Setter
     */
    public static void setUrl(String url) {
        GlobalConfig.url = url;
        System.out.println("=== [GlobalConfig] url is set to: " + url + " ===");
    }

    public static String getUrl() {
        return url;
    }
}
