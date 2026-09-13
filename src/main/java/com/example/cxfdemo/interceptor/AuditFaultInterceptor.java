package com.example.cxfdemo.interceptor;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import javax.annotation.Resource;

public class AuditFaultInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger log = LoggerFactory.getLogger(AuditFaultInterceptor.class);

    @Resource
    private com.example.cxfdemo.dao.AuditLogDao auditLogDao;

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
        
        if (auditLogDao != null) {
            auditLogDao.insertFaultLog(guid, errorMsg);
        } else {
            log.warn("AuditLogDao is not injected!");
        }
    }
}
