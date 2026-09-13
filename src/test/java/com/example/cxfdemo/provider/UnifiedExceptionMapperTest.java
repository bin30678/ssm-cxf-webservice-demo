package com.example.cxfdemo.provider;

import com.example.cxfdemo.fault.ApiError;
import com.example.cxfdemo.fault.ServiceFaultException;
import org.apache.cxf.interceptor.Fault;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class UnifiedExceptionMapperTest {

    private final UnifiedExceptionMapper mapper = new UnifiedExceptionMapper();

    @Test
    public void testToResponse_serviceFaultException() {
        ServiceFaultException ex = new ServiceFaultException("POLICY_NOT_FOUND", "查無保單資料", 404);
        Response response = mapper.toResponse(ex);

        Assert.assertEquals(404, response.getStatus());
        Assert.assertTrue(response.getEntity() instanceof ApiError);

        ApiError error = (ApiError) response.getEntity();
        Assert.assertEquals("POLICY_NOT_FOUND", error.getCode());
        Assert.assertEquals("查無保單資料", error.getMessage());
    }

    @Test
    public void testToResponse_webApplicationException() {
        WebApplicationException ex = new WebApplicationException(400);
        Response response = mapper.toResponse(ex);

        Assert.assertEquals(400, response.getStatus());
        ApiError error = (ApiError) response.getEntity();
        Assert.assertEquals("HTTP_400", error.getCode());
        Assert.assertEquals("請求路徑不正確", error.getMessage());
    }

    @Test
    public void testToResponse_cxfFault() {
        Fault fault = new Fault(new RuntimeException("Bad request"));
        fault.setStatusCode(400);

        Response response = mapper.toResponse(fault);
        Assert.assertEquals(400, response.getStatus());
        ApiError error = (ApiError) response.getEntity();
        Assert.assertEquals("HTTP_400", error.getCode());
        Assert.assertEquals("請求不正確", error.getMessage());
    }

    @Test
    public void testToResponse_generalException() {
        RuntimeException ex = new RuntimeException("Unexpected database failure");
        Response response = mapper.toResponse(ex);

        Assert.assertEquals(500, response.getStatus());
        ApiError error = (ApiError) response.getEntity();
        Assert.assertEquals("INTERNAL_ERROR", error.getCode());
        Assert.assertEquals("系統內部錯誤", error.getMessage());
    }
}
