package com.example.cxfdemo.listener;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Iterator;

public class TiffImageReaderCheckListener implements ServletContextListener {

    private boolean hasTiffReader = false;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 系統啟動時檢查是否安裝有 TIFF Reader
        Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReadersByFormatName("tiff");
        if (readers != null && readers.hasNext()) {
            hasTiffReader = true;
            System.out.println("=== [Listener Init] TIFF ImageReader is AVAILABLE in the system.");
        } else {
            System.out.println("=== [Listener Init] WARNING: TIFF ImageReader is MISSING in the system.");
        }

        if (sce != null && sce.getServletContext() != null) {
            ServletContext context = sce.getServletContext();
            context.setAttribute("hasTiffReader", hasTiffReader);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    public boolean isHasTiffReader() {
        return hasTiffReader;
    }
}
