package com.example.cxfdemo.interceptor;

import javax.xml.namespace.QName;

import com.example.cxfdemo.fault.ServiceFaultException;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
public class UnifiedFaultInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final String ERROR_NAMESPACE = "https://example.com/cxfdemo/error";

    public UnifiedFaultInterceptor() {
        super(Phase.PRE_PROTOCOL);
        addBefore("org.apache.cxf.binding.soap.interceptor.Soap11FaultOutInterceptor");
        addBefore("org.apache.cxf.binding.soap.interceptor.Soap12FaultOutInterceptor");
    }

    @Override
    public void handleMessage(Message message) {
        if (!(message instanceof SoapMessage)) {
            return;
        }
        Exception original = message.getContent(Exception.class);
        ServiceFaultException serviceFault = findCause(original, ServiceFaultException.class);
        String code = serviceFault == null ? "INTERNAL_ERROR" : serviceFault.getCode();
        String safeMessage = serviceFault == null ? "服務處理失敗" : serviceFault.getMessage();
        int status = serviceFault == null ? 500 : serviceFault.getHttpStatus();
        QName faultCode = status >= 500 ? Fault.FAULT_CODE_SERVER : Fault.FAULT_CODE_CLIENT;

        SoapFault soapFault = new SoapFault(safeMessage, original, faultCode);
        soapFault.setStatusCode(status);
        Element detail = soapFault.getOrCreateDetail();
        Document document = detail.getOwnerDocument();
        Element serviceError = document.createElementNS(ERROR_NAMESPACE, "err:ServiceError");
        append(document, serviceError, "code", code);
        append(document, serviceError, "message", safeMessage);
        Object traceValue = message.getExchange() == null ? null
                : message.getExchange().get(RequestValidationInterceptor.TRACE_ID_KEY);
        String traceId = traceValue == null ? null : traceValue.toString();
        append(document, serviceError, "traceId", traceId == null ? "" : traceId);
        detail.appendChild(serviceError);
        message.setContent(Exception.class, soapFault);
    }

    private void append(Document document, Element parent, String name, String value) {
        Element child = document.createElementNS(ERROR_NAMESPACE, "err:" + name);
        child.setTextContent(value);
        parent.appendChild(child);
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
