package com.example.cxfdemo.interceptor;

import com.example.cxfdemo.dao.AuditLogDao;
import com.example.cxfdemo.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.UUID;

/**
 * Spring MVC 審計攔截器
 * 確保打到 Spring MVC 控制器 (例如 /tasks/*) 的請求亦能被記錄至 audit_request 與 audit_response 表。
 */
public class AuditMvcInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuditMvcInterceptor.class);
    public static final String AUDIT_GUID = "AUDIT_GUID";

    @Resource
    private AuditLogDao auditLogDao;

    public void setAuditLogDao(AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String guid = request.getHeader("guid");
        if (guid == null || guid.trim().isEmpty()) {
            guid = UUID.randomUUID().toString();
        }
        request.setAttribute(AUDIT_GUID, guid);

        String ip = WebUtils.getClientIp(request);
        String clientType = WebUtils.getClientType(request);
        String hostname = WebUtils.getLocalHostname();
        String uri = request.getRequestURI();
        String method = request.getMethod();

        String payload = null;
        if (request.getQueryString() != null) {
            payload = "?" + request.getQueryString();
        }

        log.info("=== [Spring MVC Audit Request] ===");
        log.info("GUID: {}", guid);
        log.info("Time: {}", new Date());
        log.info("Client IP: {}", ip);
        log.info("URI: {} {}", method, uri);
        if (payload != null) {
            log.info("Query: {}", payload);
        }
        log.info("==================================");

        if (auditLogDao != null) {
            auditLogDao.insertRequestLog(guid, ip, clientType, hostname, uri, method, payload);
        } else {
            log.warn("AuditLogDao is not injected in AuditMvcInterceptor!");
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // No-op
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String guid = (String) request.getAttribute(AUDIT_GUID);
        if (guid == null) {
            return;
        }

        int statusCode = (ex != null) ? 500 : 200;
        try {
            java.lang.reflect.Method getStatusMethod = response.getClass().getMethod("getStatus");
            Object code = getStatusMethod.invoke(response);
            if (code instanceof Integer) {
                statusCode = (Integer) code;
            }
        } catch (Throwable ignored) {
            // Servlet 2.5 相容，使用預設值
        }

        log.info("=== [Spring MVC Audit Response] ===");
        log.info("GUID: {}", guid);
        log.info("Status: {}", statusCode);
        log.info("===================================");

        if (auditLogDao != null) {
            auditLogDao.insertResponseLog(guid, statusCode, "Spring MVC Response");
            if (ex != null) {
                auditLogDao.insertFaultLog(guid, ex.getMessage() != null ? ex.getMessage() : ex.toString());
            }
        }
    }
}