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

    /** 词根词缀 */
    private String root;

    /** 近义词 */
    private String synonyms;

    /** 反义词 */
    private String antonyms;

    /** 单词变形 */
    private String wordForms;

    /** 当前用户是否已学 */
    private Boolean isLearned;

    /** 当前用户是否已掌握 */
    private Boolean isMastered;

    /** 当前用户是否已收藏 */
    private Boolean isFavorite;

    private LocalDateTime createdAt;
}
