package com.example.cxfdemo.interceptor;

import com.example.cxfdemo.utils.WebUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuditRequestInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger log = LoggerFactory.getLogger(AuditRequestInterceptor.class);
    public static final String AUDIT_GUID = "AUDIT_GUID";

    @Resource
    private com.example.cxfdemo.dao.AuditLogDao auditLogDao;

    public AuditRequestInterceptor() {
        super(Phase.RECEIVE);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        HttpServletRequest request = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
        
        String guid = null;
        Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        if (headers != null && headers.containsKey("guid")) {
            List<String> guidList = headers.get("guid");
            if (guidList != null && !guidList.isEmpty()) {
                guid = guidList.get(0);
            }
        }
        
        if (guid == null || guid.trim().isEmpty()) {
            guid = UUID.randomUUID().toString();
        }
        
        message.getExchange().put(AUDIT_GUID, guid);

        String ip = WebUtils.getClientIp(request);
        String clientType = WebUtils.getClientType(request);
        String hostname = WebUtils.getLocalHostname();
        String uri = (String) message.get(Message.REQUEST_URI);
        String method = (String) message.get(Message.HTTP_REQUEST_METHOD);

        log.info("=== [Audit Request] ===");
        log.info("GUID: {}", guid);
        log.info("Time: {}", new Date());
        log.info("Client IP: {}", ip);
        log.info("Client Type: {}", clientType);
        log.info("Host: {}", hostname);
        log.info("URI: {} {}", method, uri);
        log.info("=======================");
        
        if (auditLogDao != null) {
            auditLogDao.insertRequestLog(guid, ip, clientType, hostname, uri, method);
        } else {
            log.warn("AuditLogDao is not injected!");
        }
    }
}
