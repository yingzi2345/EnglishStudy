package com.guet.englishcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员（tb_admin），密码使用 BCrypt 哈希（与 Django 侧 bcrypt 兼容）
 */
@Data
@TableName("tb_admin")
public class Admin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 密码哈希 */
    private String passwordHash;

    /** 角色：admin / superadmin */
    private String role;

    private String nickname;

    /** 状态：1正常, 0禁用 */
    private Integer status;

    private LocalDateTime lastLoginAt;

    private String lastLoginIp;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
