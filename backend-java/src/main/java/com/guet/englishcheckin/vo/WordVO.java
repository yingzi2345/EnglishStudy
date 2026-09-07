package com.guet.englishcheckin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单词信息（附带当前用户学习状态），对应 Django WordSerializer
 */
@Data
public class WordVO {

    private Long id;
    private String word;
    private String phonetic;
    private String meaning;
    private String exampleEn;
    private String exampleZh;
    private String audioUrl;
    private Integer level;
    private String category;

    /** 当前用户是否已学 */
    private Boolean isLearned;

    /** 当前用户是否已掌握 */
    private Boolean isMastered;

    private LocalDateTime createdAt;
}
