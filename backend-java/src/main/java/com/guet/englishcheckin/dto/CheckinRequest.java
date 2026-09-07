package com.guet.englishcheckin.dto;

import lombok.Data;

/**
 * 每日打卡请求
 */
@Data
public class CheckinRequest {

    private Integer wordCount;

    private Integer studyDuration;

    private String note;
}
