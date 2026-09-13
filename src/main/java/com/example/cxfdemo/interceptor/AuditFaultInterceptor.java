package com.example.cxfdemo.interceptor;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import javax.annotation.Resource;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

public class AuditFaultInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger log = LoggerFactory.getLogger(AuditFaultInterceptor.class);

    @Resource
    private com.example.cxfdemo.dao.AuditLogDao auditLogDao;

    public void setAuditLogDao(com.example.cxfdemo.dao.AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    private com.example.cxfdemo.dao.AuditLogDao getEffectiveDao() {
        if (this.auditLogDao != null) {
            return this.auditLogDao;
        }
        try {
            WebApplicationContext ctx = ContextLoader.getCurrentWebApplicationContext();
            if (ctx != null) {
                this.auditLogDao = ctx.getBean(com.example.cxfdemo.dao.AuditLogDao.class);
            }
        } catch (Exception e) {
            log.warn("Failed to get AuditLogDao from Spring context: {}", e.getMessage());
        }
        return this.auditLogDao;
    }

    public AuditFaultInterceptor() {
        super(Phase.MARSHAL);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        String guid = null;
        if (message.getExchange() != null) {
            guid = (String) message.getExchange().get(AuditRequestInterceptor.AUDIT_GUID);
        }
        
        Exception ex = message.getContent(Exception.class);
        String errorMsg = ex != null ? ex.getMessage() : "Unknown Fault";

        log.error("=== [Audit Fault] ===");
        log.error("GUID: {}", guid);
        log.error("Time: {}", new Date());
        log.error("Error: {}", errorMsg);
        log.error("=====================");
        
        com.example.cxfdemo.dao.AuditLogDao dao = getEffectiveDao();
        if (dao != null) {
            dao.insertFaultLog(guid, errorMsg);
        } else {
            log.warn("AuditLogDao is not injected or found!");
        }
    }
}
