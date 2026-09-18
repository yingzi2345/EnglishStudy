package com.guet.englishcheckin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户个人资料（不含 openid/status 等敏感字段），对应 Django UserProfileSerializer
 */
@Data
public class UserProfileVO {

    private Long id;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private Integer totalWords;
    private Integer totalDays;
    private Integer maxContinuous;

    /** 每日学习目标单词数 */
    private Integer dailyGoal;

    /** 当前连续打卡天数（实时计算） */
    private Integer continuousDays;

    private LocalDateTime lastLoginAt;
}
