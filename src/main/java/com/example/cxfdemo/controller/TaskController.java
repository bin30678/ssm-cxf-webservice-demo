package com.example.cxfdemo.controller;

import com.example.cxfdemo.scheduler.ScheduledTasks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 任務管理 REST 控制器 (手動打網址觸發排程工作)
 * 
 * 備註：
 * 1. 本專案使用 Spring 3.2.14.RELEASE，尚未有 @RestController 註解 (Spring 4.0 引入)。
 *    在 Spring 3.2 中，類別加上 @Controller 與 @ResponseBody 即完全等價於 @RestController。
 * 2. 透過 produces = "application/json;charset=UTF-8" 搭配 Gson 序列化，確保在內網舊版無 Jackson 環境下亦能穩定運作不報錯。
 */
@Controller
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    @Resource
    private ScheduledTasks scheduledTasks;

    private final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .setPrettyPrinting()
            .create();

    /**
     * 手動打網址觸發「清理過期備份檔案」
     * 支援 GET / POST 呼叫
     * 
     * 範例網址：
     * http://localhost:8080/tasks/clean-backup
     * http://localhost:8080/tasks/clean-backup?dir=C:/backup_folder&days=7
     * 
     * @param dir 自訂備份路徑 (選填，預設 C:/backup_folder)
     * @param days 保留天數 (選填，預設 7 天)
     * @return JSON 格式執行結果
     */
    @RequestMapping(
            value = {"/tasks/clean-backup", "/tasks/cleanBackup", "/api/tasks/clean-backup"},
            method = {RequestMethod.GET, RequestMethod.POST},
            produces = "application/json;charset=UTF-8"
    )
    @ResponseBody
    public String cleanBackup(
            @RequestParam(value = "dir", required = false, defaultValue = "C:/backup_folder") String dir,
            @RequestParam(value = "days", required = false, defaultValue = "7") Integer days
    ) {
        log.info("TaskController.cleanBackup called with dir: {}, days: {}", dir, days);
        int daysToKeep = (days != null && days > 0) ? days : 7;
        Map<String, Object> result = scheduledTasks.cleanOldBackupFiles(dir, daysToKeep);
        log.info("TaskController.cleanBackup completed with result: {}", result);
        return gson.toJson(result);
    }
}
