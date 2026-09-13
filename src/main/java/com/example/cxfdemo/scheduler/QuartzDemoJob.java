package com.example.cxfdemo.scheduler;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Date;

public class QuartzDemoJob extends QuartzJobBean {

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        System.out.println("=== [Quartz Job] Triggered at: " + new Date() + " ===");
        // 實作商業邏輯
    }
}
