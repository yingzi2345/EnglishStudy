package com.guet.englishcheckin.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 测验结果
 */
@Data
public class QuizResultVO {

    private Integer total;
    private Integer correct;
    private Integer wrong;
    private Integer score;

    /** 每题详情 */
    private List<Map<String, Object>> details;

    /** 答错的单词ID（用于跳转错词本） */
    private List<Long> wrongWordIds;
}
