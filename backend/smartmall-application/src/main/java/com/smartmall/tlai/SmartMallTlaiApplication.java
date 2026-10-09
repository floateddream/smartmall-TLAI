package com.smartmall.tlai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 多模块后端的唯一启动入口。 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class SmartMallTlaiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartMallTlaiApplication.class, args);
    }
}
