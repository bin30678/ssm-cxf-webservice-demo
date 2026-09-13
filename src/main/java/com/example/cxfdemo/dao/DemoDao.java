package com.example.cxfdemo.dao;

import java.util.List;
import java.util.Map;

// 1. MyBatis 原本直接 Call Interface 的方式
public interface DemoDao {

    List<Map<String, Object>> queryFromDb1(String param);

}
