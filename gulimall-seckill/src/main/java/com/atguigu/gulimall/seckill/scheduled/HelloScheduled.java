package com.atguigu.gulimall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author: CyS2020
 * @date: 2021/11/30
 */
@Slf4j
@Component
//@EnableAsync
//@EnableScheduling
public class HelloScheduled {

    //@Async
    //@Scheduled(cron = "* * * * *  ?")
    public void hello() throws InterruptedException {
        log.info("hello...");
        TimeUnit.SECONDS.sleep(3);
    }
}
