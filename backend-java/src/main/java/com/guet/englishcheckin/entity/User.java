package com.guet.englishcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 微信小程序用户（tb_user）
 */
@Data
@TableName("tb_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 微信 OpenID */
    private String openid;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatarUrl;

    /** 性别：0未知, 1男, 2女 */
    private Integer gender;

    /** 状态：1正常, 0禁用 */
    private Integer status;

    /** 累计学习单词数 */
    private Integer totalWords;

    /** 累计打卡天数 */
    private Integer totalDays;

    /** 最长连续打卡天数 */
    private Integer maxContinuous;

    /** 注册时间 */
    private LocalDateTime createdAt;

    /** 最近登录时间 */
    private LocalDateTime lastLoginAt;

    /** 最近登录 IP */
    private String lastLoginIp;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
