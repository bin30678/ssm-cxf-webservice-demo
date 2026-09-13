package com.example.cxfdemo.dao;

import java.util.List;
import java.util.Map;

// 1. MyBatis ?�本?�接 Call Interface ?�方�?
public interface DemoDao {
    
    List<Map<String, Object>> queryFromDb1(String param);
    
}
