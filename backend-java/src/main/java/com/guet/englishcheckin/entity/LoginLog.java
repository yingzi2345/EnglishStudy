package com.guet.englishcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志（tb_login_log）— 网络工程 IP 溯源特色
 */
@Data
@TableName("tb_login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 登录 IP 地址 */
    private String ipAddress;

    /** 设备信息（User-Agent） */
    private String deviceInfo;

    /** IP 归属地 */
    private String location;

    /** 登录时间 */
    private LocalDateTime loginTime;

    /** 是否异常登录：0正常, 1异常 */
    private Integer isAbnormal;

    /** 异常原因 */
    private String abnormalReason;

    private LocalDateTime createdAt;
}
