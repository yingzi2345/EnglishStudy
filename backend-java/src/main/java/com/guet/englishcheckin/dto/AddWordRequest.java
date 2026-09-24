package com.guet.englishcheckin.dto;

import lombok.Data;

/**
 * 手动添加单词到自定义词本
 */
@Data
public class AddWordRequest {
    private String word;
    private String phonetic;
    private String meaning;
    private String exampleEn;
    private String exampleZh;
}
