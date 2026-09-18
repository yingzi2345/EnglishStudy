package com.guet.englishcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户已解锁徽章（tb_user_achievement）
 */
@Data
@TableName("tb_user_achievement")
public class UserAchievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long achievementId;

    private LocalDateTime unlockedAt;
}
