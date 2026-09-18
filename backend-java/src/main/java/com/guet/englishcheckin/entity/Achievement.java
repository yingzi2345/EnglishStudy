package com.guet.englishcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成就徽章定义（tb_achievement）
 */
@Data
@TableName("tb_achievement")
public class Achievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String icon;

    /** 条件类型: continuous_days / total_words / total_days */
    private String conditionType;

    private Integer conditionValue;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
