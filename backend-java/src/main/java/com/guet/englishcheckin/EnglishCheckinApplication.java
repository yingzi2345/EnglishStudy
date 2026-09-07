package com.guet.englishcheckin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 英语学习打卡系统 — Spring Boot 启动类
 * 原 Django 后端（backend/）迁移而来，接口路径与响应格式保持兼容。
 */
@SpringBootApplication
@MapperScan("com.guet.englishcheckin.mapper")
public class EnglishCheckinApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishCheckinApplication.class, args);
    }
}
