package com.guet.englishcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户单词学习进度（tb_word_progress），(user_id, word_id) 唯一
 */
@Data
@TableName("tb_word_progress")
public class WordProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long wordId;

    /** 是否已学：0未学, 1已学 */
    private Integer isLearned;

    /** 是否掌握：0未掌握, 1已掌握 */
    private Integer isMastered;

    /** 学习时间（UTC） */
    private LocalDateTime learnedAt;

    /** 复习次数 */
    private Integer reviewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
