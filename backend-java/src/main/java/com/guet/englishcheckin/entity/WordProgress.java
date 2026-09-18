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

    /** 下次复习时间（UTC） */
    private LocalDateTime nextReviewAt;

    /** 艾宾浩斯间隔等级：0=1天,1=2天,2=4天,3=7天,4=15天,5=30天(已掌握) */
    private Integer intervalLevel;

    /** 答错次数 */
    private Integer wrongCount;

    /** 是否在错词本：0否,1是 */
    private Integer isWrong;

    /** 上次学习时间（UTC） */
    private LocalDateTime lastStudyAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
