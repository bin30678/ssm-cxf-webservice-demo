package com.example.cxfdemo.filter;

import javax.imageio.ImageIO;
import javax.servlet.*;
import java.io.IOException;
import java.util.Iterator;

public class TiffImageReaderCheckFilter implements Filter {

    private boolean hasTiffReader = false;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 系統?��??�檢?�是?��?裝�? TIFF Reader
        Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReadersByFormatName("tiff");
        if (readers != null && readers.hasNext()) {
            hasTiffReader = true;
            System.out.println("=== [Filter Init] TIFF ImageReader is AVAILABLE in the system.");
        } else {
            System.out.println("=== [Filter Init] WARNING: TIFF ImageReader is MISSING in the system.");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!hasTiffReader) {
            System.out.println("WARNING: Processing request but TIFF ImageReader is missing.");
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
