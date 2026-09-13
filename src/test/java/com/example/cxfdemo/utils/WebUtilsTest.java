package com.example.cxfdemo.utils;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;

public class WebUtilsTest {

    @Test
    public void testGetClientIp_withNullRequest_shouldReturnUnknown() {
        String ip = WebUtils.getClientIp(null);
        Assert.assertEquals("unknown", ip);
    }

    @Test
    public void testGetClientIp_withXForwardedFor_shouldReturnFirstIp() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1");

        String ip = WebUtils.getClientIp(request);
        Assert.assertEquals("192.168.1.100", ip);
    }

    @Test
    public void testGetClientIp_withProxyClientIp_shouldReturnIp() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Proxy-Client-IP")).thenReturn("10.1.2.3");

        String ip = WebUtils.getClientIp(request);
        Assert.assertEquals("10.1.2.3", ip);
    }

    @Test
    public void testGetClientIp_fallbackToRemoteAddr() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String ip = WebUtils.getClientIp(request);
        Assert.assertEquals("127.0.0.1", ip);
    }

    @Test
    public void testGetClientType_variousUserAgents() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // Null request
        Assert.assertEquals("unknown", WebUtils.getClientType(null));

        // Postman
        Mockito.when(request.getHeader("User-Agent")).thenReturn("PostmanRuntime/7.29.0");
        Assert.assertEquals("Postman", WebUtils.getClientType(request));

        // Curl
        Mockito.when(request.getHeader("User-Agent")).thenReturn("curl/7.68.0");
        Assert.assertEquals("HttpClient", WebUtils.getClientType(request));

        // Apache-HttpClient
        Mockito.when(request.getHeader("User-Agent")).thenReturn("Apache-HttpClient/4.5.2 (Java/1.8.0_292)");
        Assert.assertEquals("HttpClient", WebUtils.getClientType(request));

        // Mobile App
        Mockito.when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)");
        Assert.assertEquals("App", WebUtils.getClientType(request));

        // Browser
        Mockito.when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/90.0.4430.212");
        Assert.assertEquals("Browser", WebUtils.getClientType(request));

        // Other
        Mockito.when(request.getHeader("User-Agent")).thenReturn("CustomTool/1.0");
        Assert.assertEquals("Other", WebUtils.getClientType(request));
    }

    @Test
    public void testGetLocalHostname() {
        String hostname = WebUtils.getLocalHostname();
        Assert.assertNotNull(hostname);
        Assert.assertFalse(hostname.isEmpty());
    }
}
