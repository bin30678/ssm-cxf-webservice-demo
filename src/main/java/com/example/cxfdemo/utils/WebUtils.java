package com.example.cxfdemo.utils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class WebUtils {

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果有多級代理，取第一個IP為真實IP
        if (ip != null && ip.indexOf(',') != -1) {
            ip = ip.substring(0, ip.indexOf(',')).trim();
        }
        return ip;
    }

    public static String getClientType(HttpServletRequest request) {
        if (request == null) return "unknown";
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return "unknown";
        
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("postman")) {
            return "Postman";
        } else if (userAgent.contains("apache-httpclient") || userAgent.contains("java") || userAgent.contains("curl")) {
            return "HttpClient";
        } else if (userAgent.contains("okhttp") || userAgent.contains("android") || userAgent.contains("iphone") || userAgent.contains("app")) {
            return "App";
        } else if (userAgent.contains("mozilla") || userAgent.contains("chrome") || userAgent.contains("safari")) {
            return "Browser";
        }
        return "Other";
    }

    public static String getLocalHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-host";
        }
    }
}
