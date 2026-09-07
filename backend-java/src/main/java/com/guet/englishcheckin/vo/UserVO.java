package com.guet.englishcheckin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息（管理端/列表用），对应 Django UserSerializer
 */
@Data
public class UserVO {

    private Long id;
    private String openid;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private Integer status;
    private Integer totalWords;
    private Integer totalDays;
    private Integer maxContinuous;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
