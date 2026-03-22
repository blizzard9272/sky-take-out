package com.sky.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 自定义定时任务类
 */
@Component
@Slf4j
public class MyTask {


//    @Scheduled(cron = "0/5 * * * * ?") // 每5秒执行一次
//    public void myScheduledTask() {
//        log.info("执行定时任务：{}", new Date());
//    }
}
