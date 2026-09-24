package com.guet.englishcheckin.dto;

import lombok.Data;

/**
 * 文本粘贴批量导入单词
 * 支持格式（每行一个）：
 *   word,中文释义
 *   word - 中文释义
 *   word：中文释义
 *   word（仅单词，释义留空）
 */
@Data
public class ImportWordsRequest {
    private String text;
}
