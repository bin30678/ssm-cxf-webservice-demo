package com.example.cxfdemo.controller;

import com.example.cxfdemo.scheduler.ScheduledTasks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 任�?管�? REST ?�制??(?��??�網?�觸發?��?工�?)
 * 
 * ?�註�? * 1. ?��?案使??Spring 3.2.14.RELEASE，�??��? @RestController 註解 (Spring 4.0 引入)?? *    ??Spring 3.2 中�?類別?��? @Controller ??@ResponseBody ?��??��??�於 @RestController?? * 2. ?��? produces = "application/json;charset=UTF-8" ?��? Gson 序�??��?確�??�內網�??�無 Jackson ?��?下亦?�穩定�?作�??�錯?? */
@Controller
public class TaskController {

    @Resource
    private ScheduledTasks scheduledTasks;

    private final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .setPrettyPrinting()
            .create();

    /**
     * ?��??�網?�觸發?��??��??��?份�?案�?     * ?�援 GET / POST ?�叫
     * 
     * 範�?網�?�?     * http://localhost:8080/tasks/clean-backup
     * http://localhost:8080/tasks/clean-backup?dir=C:/backup_folder&days=7
     * 
     * @param dir ?��??�份路�? (?�填，�?�?C:/backup_folder)
     * @param days 保�?天數 (?�填，�?�?7 �?
     * @return JSON ?��??��?結�?
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
        int daysToKeep = (days != null && days > 0) ? days : 7;
        Map<String, Object> result = scheduledTasks.cleanOldBackupFiles(dir, daysToKeep);
        return gson.toJson(result);
    }
}
