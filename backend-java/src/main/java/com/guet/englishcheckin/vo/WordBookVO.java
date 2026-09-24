package com.guet.englishcheckin.vo;

import lombok.Data;

/**
 * 词书 / 自定义词本 视图对象
 */
@Data
public class WordBookVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private String bookGroup;
    private String bookType;
    private Integer sortOrder;

    /** 总词量 */
    private Integer totalWords;
    /** 已学词数 */
    private Integer learnedWords;
    /** 未学词数 */
    private Integer unlearnedWords;
    /** 预计完成天数（按每日目标估算） */
    private Integer estimatedDays;

    /** 当前用户是否已加入 */
    private Boolean isJoined;
    /** 是否为当前学习词书 */
    private Boolean isCurrent;
}
