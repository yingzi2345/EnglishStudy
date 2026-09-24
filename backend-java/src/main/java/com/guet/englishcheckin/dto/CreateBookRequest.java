package com.guet.englishcheckin.dto;

import lombok.Data;

/**
 * 创建自定义词本请求
 */
@Data
public class CreateBookRequest {
    private String name;
    private String description;
    private String icon;
}
