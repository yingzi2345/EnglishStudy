package com.guet.englishcheckin.vo;

import lombok.Data;

/**
 * 学习任务卡片（今日学习任务中的单个单词）
 */
@Data
public class StudyTaskVO {

    private Long wordId;

    /** 任务类型：new=新词, review=复习 */
    private String taskType;

    private String word;
    private String phonetic;
    private String meaning;
    private String exampleEn;
    private String exampleZh;
    private String audioUrl;

    /** 当前用户是否已收藏该词 */
    private Boolean isFavorite;
}
