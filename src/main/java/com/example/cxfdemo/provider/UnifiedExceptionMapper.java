package com.example.cxfdemo.provider;

import com.example.cxfdemo.fault.ApiError;
import com.example.cxfdemo.fault.ServiceFaultException;
import com.example.cxfdemo.interceptor.RequestValidationInterceptor;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.springframework.stereotype.Component;

@Component
@Provider
public class UnifiedExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable throwable) {
        ServiceFaultException serviceFault = findCause(throwable, ServiceFaultException.class);
        int status;
        String code;
        String message;
        if (serviceFault != null) {
            status = serviceFault.getHttpStatus();
            code = serviceFault.getCode();
            message = serviceFault.getMessage();
        } else if (throwable instanceof WebApplicationException) {
            WebApplicationException webException = (WebApplicationException) throwable;
            status = webException.getResponse().getStatus();
            code = "HTTP_" + status;
            message = status >= 500 ? "系統內部錯誤" : "請求路徑不正確";
        } else if (throwable instanceof Fault) {
            Fault fault = (Fault) throwable;
            status = fault.getStatusCode();
            code = "HTTP_" + status;
            message = status >= 500 ? "系統內部錯誤" : "請求不正確";
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            code = "INTERNAL_ERROR";
            message = "系統內部錯誤";
        }

        String traceId = null;
        org.apache.cxf.message.Message currentMessage = JAXRSUtils.getCurrentMessage();
        if (currentMessage != null && currentMessage.getExchange() != null) {
            Object traceIdObj = currentMessage.getExchange().get(RequestValidationInterceptor.TRACE_ID_KEY);
            if (traceIdObj != null) {
                traceId = traceIdObj.toString();
            }
        }

        ApiError apiError = new ApiError(code, message, traceId);

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(apiError)
                .build();
    }

    private <T extends Throwable> T findCause(Throwable t, Class<T> type) {
        while (t != null) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
            t = t.getCause();
        }
        return null;
    }
}
