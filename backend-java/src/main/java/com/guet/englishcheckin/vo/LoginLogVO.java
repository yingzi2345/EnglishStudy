package com.guet.englishcheckin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志（含用户昵称），对应 Django LoginLogSerializer
 */
@Data
public class LoginLogVO {

    private Long id;
    private Long user;
    private String nickname;
    private String ipAddress;
    private String deviceInfo;
    private String location;
    private LocalDateTime loginTime;
    private Integer isAbnormal;
    private String abnormalReason;
}
