package com.example.cxfdemo.listener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

public class FontCheckListener implements ServletContextListener {

    private String targetFont;
    private boolean fontExists = false;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce != null ? sce.getServletContext() : null;
        if (context != null) {
            targetFont = context.getInitParameter("targetFont");
        }
        if (targetFont == null || targetFont.trim().isEmpty()) {
            targetFont = "Arial"; // 預設檢查 Arial
        }

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontExists = Arrays.asList(fonts).contains(targetFont);

        if (fontExists) {
            System.out.println("=== [Listener Init] Font '" + targetFont + "' is AVAILABLE in the system.");
        } else {
            System.out.println("=== [Listener Init] WARNING: Font '" + targetFont + "' is MISSING in the system.");
        }

        if (context != null) {
            context.setAttribute("fontExists", fontExists);
            context.setAttribute("targetFont", targetFont);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    public String getTargetFont() {
        return targetFont;
    }

    public boolean isFontExists() {
        return fontExists;
    }
}
