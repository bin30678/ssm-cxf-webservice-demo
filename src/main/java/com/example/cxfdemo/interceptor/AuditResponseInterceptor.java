package com.example.cxfdemo.interceptor;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import javax.annotation.Resource;

public class AuditResponseInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger log = LoggerFactory.getLogger(AuditResponseInterceptor.class);

    @Resource
    private com.example.cxfdemo.dao.AuditLogDao auditLogDao;

    public AuditResponseInterceptor() {
        // PRE_STREAM 階段才能攔截底層的 OutputStream
        super(Phase.PRE_STREAM);
        // 確保在寫出前執行
        addBefore(StaxOutInterceptor.class.getName());
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        OutputStream os = message.getContent(OutputStream.class);
        if (os == null) {
            return;
        }

        // 將原始 OutputStream 包裝起來，這樣能將資料寫入記憶體緩衝區
        CacheAndWriteOutputStream newOut = new CacheAndWriteOutputStream(os);
        message.setContent(OutputStream.class, newOut);

        // 註冊回呼 (Callback)，當 Stream onClose() 時就會觸發
        newOut.registerCallback(new LoggingCallback(message, os, auditLogDao));
    }

    private static class LoggingCallback implements CachedOutputStreamCallback {
        private final Message message;
        private final OutputStream origStream;
        private final com.example.cxfdemo.dao.AuditLogDao auditLogDao;

        public LoggingCallback(Message message, OutputStream origStream, com.example.cxfdemo.dao.AuditLogDao auditLogDao) {
            this.message = message;
            this.origStream = origStream;
            this.auditLogDao = auditLogDao;
        }

        @Override
        public void onFlush(CachedOutputStream cos) {
            // Do nothing
        }

        @Override
        public void onClose(CachedOutputStream cos) {
            try {
                String guid = null;
                if (message.getExchange() != null) {
                    guid = (String) message.getExchange().get(AuditRequestInterceptor.AUDIT_GUID);
                }
                
                Integer responseCode = (Integer) message.get(Message.RESPONSE_CODE);

                // 從緩衝區中讀取出實際寫入的 Payload
                StringBuilder payload = new StringBuilder();
                try (InputStream is = cos.getInputStream()) {
                    byte[] bytes = new byte[is.available()];
                    is.read(bytes);
                    payload.append(new String(bytes, "UTF-8"));
                } catch (Exception e) {
                    payload.append("Error reading payload: ").append(e.getMessage());
                }

                log.info("=== [Audit Response (onClose)] ===");
                log.info("GUID: {}", guid);
                log.info("Time: {}", new Date());
                log.info("Response Code: {}", responseCode);
                log.info("Payload: {}", payload.toString());
                log.info("==================================");
                
                if (auditLogDao != null) {
                    auditLogDao.insertResponseLog(guid, responseCode);
                } else {
                    log.warn("AuditLogDao is not injected!");
                }

            } catch (Exception e) {
                log.error("AuditResponseInterceptor callback error", e);
            }
        }
    }
}
