package com.example.cxfdemo.filter;

import javax.servlet.*;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.Arrays;

public class FontCheckFilter implements Filter {

    private String targetFont;
    private boolean fontExists = false;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        targetFont = filterConfig.getInitParameter("targetFont");
        if (targetFont == null || targetFont.trim().isEmpty()) {
            targetFont = "Arial"; // 預設檢查 Arial
        }

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontExists = Arrays.asList(fonts).contains(targetFont);
        
        if (fontExists) {
            System.out.println("=== [Filter Init] Font '" + targetFont + "' is AVAILABLE in the system.");
        } else {
            System.out.println("=== [Filter Init] WARNING: Font '" + targetFont + "' is MISSING in the system.");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!fontExists) {
            System.out.println("WARNING: Processing request but target font '" + targetFont + "' is missing.");
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
