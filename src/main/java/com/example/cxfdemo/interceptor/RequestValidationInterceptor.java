package com.example.cxfdemo.interceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.example.cxfdemo.fault.ServiceFaultException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RequestValidationInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger log = LoggerFactory.getLogger(RequestValidationInterceptor.class);

    public static final String TRACE_ID_KEY = RequestValidationInterceptor.class.getName() + ".traceId";
    public static final String START_NANOS_KEY = RequestValidationInterceptor.class.getName() + ".startNanos";

    private static final List<String> BODYLESS_METHODS = Arrays.asList("GET", "HEAD", "OPTIONS");

    public RequestValidationInterceptor() {
        super(Phase.RECEIVE);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        String traceId = requestTraceId(message);
        log.info("RequestValidationInterceptor.handleMessage validating request with traceId: {}", traceId);
        if (message.getExchange() != null) {
            message.getExchange().put(START_NANOS_KEY, System.nanoTime());
            message.getExchange().put(TRACE_ID_KEY, traceId);
        }

        // These properties are read by CXF's StAX interceptor before parsing SOAP XML.
        message.put(StaxUtils.ALLOW_INSECURE_PARSER, Boolean.FALSE);
        message.put(StaxUtils.MAX_ELEMENT_DEPTH, 50);
        message.put(StaxUtils.MAX_CHILD_ELEMENTS, 10_000);
        message.put(StaxUtils.MAX_ATTRIBUTE_COUNT, 100);
        message.put(StaxUtils.MAX_TEXT_LENGTH, 1_048_576);
        message.put(StaxUtils.MAX_ELEMENT_COUNT, 100_000);
        message.put(StaxUtils.MAX_XML_CHARACTERS, 2_097_152L);

        String method = stringValue(message.get(Message.HTTP_REQUEST_METHOD)).toUpperCase(Locale.ROOT);
        if (BODYLESS_METHODS.contains(method)) {
            return;
        }

        String contentType = baseMediaType(stringValue(message.get(Message.CONTENT_TYPE)));
        boolean soap = message instanceof SoapMessage;
        boolean allowed = soap ? isSoap(contentType) : isJson(contentType);
        if (!allowed) {
            log.warn("Unsupported Media Type in RequestValidationInterceptor: {}, soap: {}", contentType, soap);
            ServiceFaultException exception = new ServiceFaultException(
                    "UNSUPPORTED_MEDIA_TYPE",
                    soap ? "SOAP 僅接受 text/xml 或 application/soap+xml"
                            : "REST 僅接受 application/json",
                    415);
            Fault fault = new Fault(exception);
            fault.setStatusCode(415);
            throw fault;
        }
    }

    private String requestTraceId(Message message) {
        Object rawHeaders = message.get(Message.PROTOCOL_HEADERS);
        if (rawHeaders instanceof Map) {
            Map<?, ?> headers = (Map<?, ?>) rawHeaders;
            for (Map.Entry<?, ?> entry : headers.entrySet()) {
                if ("x-request-id".equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                    String candidate = firstHeaderValue(entry.getValue());
                    if (candidate.matches("[A-Za-z0-9._-]{8,64}")) {
                        return candidate;
                    }
                }
            }
        }
        return UUID.randomUUID().toString();
    }

    private String firstHeaderValue(Object value) {
        if (value instanceof Iterable) {
            Iterable<?> values = (Iterable<?>) value;
            for (Object item : values) {
                return item == null ? "" : item.toString();
            }
        }
        return value == null ? "" : value.toString();
    }

    private String baseMediaType(String contentType) {
        int separator = contentType.indexOf(';');
        return (separator < 0 ? contentType : contentType.substring(0, separator))
                .trim().toLowerCase(Locale.ROOT);
    }

    private boolean isJson(String contentType) {
        return "application/json".equals(contentType)
                || contentType.startsWith("application/") && contentType.endsWith("+json");
    }

    private boolean isSoap(String contentType) {
        return "text/xml".equals(contentType) || "application/soap+xml".equals(contentType);
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
