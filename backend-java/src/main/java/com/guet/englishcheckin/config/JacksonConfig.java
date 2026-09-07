package com.guet.englishcheckin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 序列化配置（Spring Boot 4 使用 Jackson 3，API 为 tools.jackson.*）：
 * 1. LocalDateTime 输出 ISO-8601 并带 UTC 标记（与 Django DRF 的 "+00:00" 行为一致，前端 new Date() 可正确解析）
 * 2. 全局字段命名策略 SNAKE_CASE 在 application.yml 配置（spring.jackson.property-naming-strategy）
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter ISO_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Bean
    public JacksonModule localDateTimeSerializerModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new ValueSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                // 存储值即 UTC，直接追加 Z 标记
                gen.writeString(value.format(ISO_UTC) + "Z");
            }
        });
        return module;
    }
}
