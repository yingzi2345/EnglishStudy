package com.guet.englishcheckin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 标记单词学习状态请求
 */
@Data
public class MarkWordRequest {

    @NotNull(message = "word_id 不能为空")
    private Long wordId;

    private Boolean isMastered;
}
