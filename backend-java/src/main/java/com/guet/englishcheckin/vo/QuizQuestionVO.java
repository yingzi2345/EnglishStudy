package com.guet.englishcheckin.vo;

import lombok.Data;

import java.util.List;

/**
 * 测验题目
 */
@Data
public class QuizQuestionVO {

    private Long wordId;
    private String word;
    private String phonetic;
    private String meaning;
    private String audioUrl;

    /** 题型：listening=听音选词, meaning=看词选义, spelling=看义拼写 */
    private String questionType;

    /** 选择题选项（拼写题为空） */
    private List<String> options;
}
