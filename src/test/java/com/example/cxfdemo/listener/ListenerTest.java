package com.example.cxfdemo.listener;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

public class ListenerTest {

    @Test
    public void testTiffImageReaderCheckListener() {
        TiffImageReaderCheckListener listener = new TiffImageReaderCheckListener();
        ServletContextEvent mockEvent = Mockito.mock(ServletContextEvent.class);
        ServletContext mockContext = Mockito.mock(ServletContext.class);
        Mockito.when(mockEvent.getServletContext()).thenReturn(mockContext);

        // 執行生命週期初始化
        listener.contextInitialized(mockEvent);

        // 驗證屬性寫入
        Mockito.verify(mockContext, Mockito.times(1)).setAttribute(Mockito.eq("hasTiffReader"), Mockito.anyBoolean());

        // 測試關閉回調
        listener.contextDestroyed(mockEvent);
    }

    @Test
    public void testFontCheckListener_withDefaultFont() {
        FontCheckListener listener = new FontCheckListener();
        ServletContextEvent mockEvent = Mockito.mock(ServletContextEvent.class);
        ServletContext mockContext = Mockito.mock(ServletContext.class);
        Mockito.when(mockEvent.getServletContext()).thenReturn(mockContext);
        Mockito.when(mockContext.getInitParameter("targetFont")).thenReturn(null);

        listener.contextInitialized(mockEvent);

        Assert.assertEquals("Arial", listener.getTargetFont());
        Mockito.verify(mockContext, Mockito.times(1)).setAttribute(Mockito.eq("targetFont"), Mockito.eq("Arial"));
        Mockito.verify(mockContext, Mockito.times(1)).setAttribute(Mockito.eq("fontExists"), Mockito.anyBoolean());

        listener.contextDestroyed(mockEvent);
    }

    @Test
    public void testFontCheckListener_withCustomFont() {
        FontCheckListener listener = new FontCheckListener();
        ServletContextEvent mockEvent = Mockito.mock(ServletContextEvent.class);
        ServletContext mockContext = Mockito.mock(ServletContext.class);
        Mockito.when(mockEvent.getServletContext()).thenReturn(mockContext);
        Mockito.when(mockContext.getInitParameter("targetFont")).thenReturn("NonExistentFont_XYZ_123");

        listener.contextInitialized(mockEvent);

        Assert.assertEquals("NonExistentFont_XYZ_123", listener.getTargetFont());
        Assert.assertFalse(listener.isFontExists());
        Mockito.verify(mockContext, Mockito.times(1)).setAttribute("fontExists", false);
        Mockito.verify(mockContext, Mockito.times(1)).setAttribute("targetFont", "NonExistentFont_XYZ_123");

        listener.contextDestroyed(mockEvent);
    }
}
