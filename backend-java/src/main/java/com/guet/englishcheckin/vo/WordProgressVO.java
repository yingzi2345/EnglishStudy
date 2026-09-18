package com.guet.englishcheckin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单词学习进度（附带单词信息），对应 Django WordProgressSerializer
 */
@Data
public class WordProgressVO {

    private Long id;
    private Long user;
    private Long word;
    private String wordName;
    private String wordMeaning;
    private Integer isLearned;
    private Integer isMastered;
    private LocalDateTime learnedAt;
    private Integer reviewCount;

    /** 答错次数 */
    private Integer wrongCount;

    /** 是否在错词本 */
    private Integer isWrong;

    /** 艾宾浩斯间隔等级 */
    private Integer intervalLevel;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
