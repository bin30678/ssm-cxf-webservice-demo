package com.example.cxfdemo.interceptor;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResponseHeaderInterceptor extends AbstractPhaseInterceptor<Message> {

    public ResponseHeaderInterceptor() {
        super(Phase.PRE_STREAM);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        if (headers == null) {
            headers = new HashMap<>();
            message.put(Message.PROTOCOL_HEADERS, headers);
        }

        addHeaderIfNotExists(headers, "X-Content-Type-Options", "nosniff");
        addHeaderIfNotExists(headers, "X-XSS-Protection", "1; mode=block");
        addHeaderIfNotExists(headers, "Cache-Control", "no-store");
        addHeaderIfNotExists(headers, "Pragma", "no-cache");

        if (message.getExchange() != null) {
            Object traceId = message.getExchange().get(RequestValidationInterceptor.TRACE_ID_KEY);
            if (traceId != null) {
                addHeaderIfNotExists(headers, "X-Request-Id", traceId.toString());
            }
        }
    }

    private void addHeaderIfNotExists(Map<String, List<String>> headers, String name, String value) {
        if (!headers.containsKey(name)) {
            headers.put(name, new ArrayList<>(Arrays.asList(value)));
        }
    }
}
