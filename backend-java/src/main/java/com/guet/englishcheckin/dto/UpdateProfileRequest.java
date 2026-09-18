package com.guet.englishcheckin.dto;

import lombok.Data;

/**
 * 更新个人信息请求（仅允许昵称/头像/性别）
 */
@Data
public class UpdateProfileRequest {

    private String nickname;

    private String avatarUrl;

    private Integer gender;

    /** 每日学习目标单词数 */
    private Integer dailyGoal;
}
