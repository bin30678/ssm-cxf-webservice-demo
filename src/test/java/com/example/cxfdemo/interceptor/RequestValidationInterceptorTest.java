package com.example.cxfdemo.interceptor;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestValidationInterceptorTest {

    private RequestValidationInterceptor interceptor;

    @Before
    public void setUp() {
        interceptor = new RequestValidationInterceptor();
    }

    @Test
    public void testHandleMessage_bodylessMethod_get_shouldPass() {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.HTTP_REQUEST_METHOD, "GET");

        interceptor.handleMessage(message);

        // ?�設�?START_NANOS_KEY ??TRACE_ID_KEY
        Assert.assertNotNull(message.getExchange().get(RequestValidationInterceptor.START_NANOS_KEY));
        Assert.assertNotNull(message.getExchange().get(RequestValidationInterceptor.TRACE_ID_KEY));
    }

    @Test
    public void testHandleMessage_postWithValidJson_shouldPass() {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.HTTP_REQUEST_METHOD, "POST");
        message.put(Message.CONTENT_TYPE, "application/json; charset=UTF-8");

        interceptor.handleMessage(message);
        Assert.assertNotNull(message.getExchange().get(RequestValidationInterceptor.TRACE_ID_KEY));
    }

    @Test
    public void testHandleMessage_postWithUnsupportedMediaType_shouldThrowFault() {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.HTTP_REQUEST_METHOD, "POST");
        message.put(Message.CONTENT_TYPE, "text/plain");

        try {
            interceptor.handleMessage(message);
            Assert.fail("Should have thrown Fault for unsupported media type");
        } catch (Fault fault) {
            Assert.assertEquals(415, fault.getStatusCode());
        }
    }

    @Test
    public void testHandleMessage_customTraceIdExtracted() {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.HTTP_REQUEST_METHOD, "GET");

        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("x-request-id", Collections.singletonList("custom-trace-12345678"));
        message.put(Message.PROTOCOL_HEADERS, headers);

        interceptor.handleMessage(message);

        Object traceId = message.getExchange().get(RequestValidationInterceptor.TRACE_ID_KEY);
        Assert.assertEquals("custom-trace-12345678", traceId);
    }
}
