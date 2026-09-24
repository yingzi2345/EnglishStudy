package com.guet.englishcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-词书 关系（tb_user_book）：加入的词书、当前学习词书
 */
@Data
@TableName("tb_user_book")
public class UserBook {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookId;

    /** 是否当前学习词书（同一用户仅一本为 1） */
    private Integer isCurrent;

    private LocalDateTime addedAt;
}
