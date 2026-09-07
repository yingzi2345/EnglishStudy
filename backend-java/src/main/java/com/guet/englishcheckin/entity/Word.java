package com.guet.englishcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 英语单词（tb_word）
 */
@Data
@TableName("tb_word")
public class Word {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 单词原文 */
    private String word;

    /** 音标 */
    private String phonetic;

    /** 中文释义 */
    private String meaning;

    /** 英文例句 */
    private String exampleEn;

    /** 例句翻译 */
    private String exampleZh;

    /** 发音音频 URL */
    private String audioUrl;

    /** 难度等级：1初级, 2中级, 3高级 */
    private Integer level;

    /** 分类标签：CET-4 / CET-6 等 */
    private String category;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
