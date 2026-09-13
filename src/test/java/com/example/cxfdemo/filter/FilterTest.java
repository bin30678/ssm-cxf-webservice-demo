package com.example.cxfdemo.filter;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class FilterTest {

    @Test
    public void testTiffImageReaderCheckFilter() throws ServletException, IOException {
        TiffImageReaderCheckFilter filter = new TiffImageReaderCheckFilter();
        FilterConfig mockConfig = Mockito.mock(FilterConfig.class);

        // 初始化
        filter.init(mockConfig);

        ServletRequest request = Mockito.mock(ServletRequest.class);
        ServletResponse response = Mockito.mock(ServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        // 執行 doFilter
        filter.doFilter(request, response, chain);

        // 驗證是否呼叫後續的 filterChain
        Mockito.verify(chain, Mockito.times(1)).doFilter(request, response);

        filter.destroy();
    }

    @Test
    public void testFontCheckFilter_withDefaultFont() throws ServletException, IOException {
        FontCheckFilter filter = new FontCheckFilter();
        FilterConfig mockConfig = Mockito.mock(FilterConfig.class);
        Mockito.when(mockConfig.getInitParameter("targetFont")).thenReturn(null);

        filter.init(mockConfig);

        ServletRequest request = Mockito.mock(ServletRequest.class);
        ServletResponse response = Mockito.mock(ServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);
        Mockito.verify(chain, Mockito.times(1)).doFilter(request, response);

        filter.destroy();
    }

    @Test
    public void testFontCheckFilter_withCustomFont() throws ServletException, IOException {
        FontCheckFilter filter = new FontCheckFilter();
        FilterConfig mockConfig = Mockito.mock(FilterConfig.class);
        Mockito.when(mockConfig.getInitParameter("targetFont")).thenReturn("NonExistentFont_XYZ_123");

        filter.init(mockConfig);

        ServletRequest request = Mockito.mock(ServletRequest.class);
        ServletResponse response = Mockito.mock(ServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);
        Mockito.verify(chain, Mockito.times(1)).doFilter(request, response);

        filter.destroy();
    }
}
