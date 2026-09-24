package com.guet.englishcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 词书 / 自定义词本（tb_word_book）
 * book_type=system 官方词书（code 非空）；book_type=custom 用户自定义词本（owner_id 非空，code 为 null）
 */
@Data
@TableName("tb_word_book")
public class WordBook {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 官方词书唯一编码：CET4 / CET6 ...；自定义词本为 null */
    private String code;

    private String name;

    private String description;

    /** 图标 emoji */
    private String icon;

    /** 分组：中小学 / 大学 / 应试 / 通用 / 自定义 */
    private String bookGroup;

    /** system / custom */
    private String bookType;

    /** 自定义词本归属用户；官方词书为 null */
    private Long ownerId;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
