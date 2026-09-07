package com.guet.englishcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日打卡记录（tb_checkin），(user_id, checkin_date) 唯一
 */
@Data
@TableName("tb_checkin")
public class Checkin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 打卡日期（东八区日期） */
    private LocalDate checkinDate;

    /** 打卡时间（UTC） */
    private LocalDateTime checkinTime;

    /** 当日学习单词数 */
    private Integer wordCount;

    /** 当日学习时长（分钟） */
    private Integer studyDuration;

    /** 截至当日连续打卡天数 */
    private Integer continuousDays;

    /** 打卡备注 */
    private String note;

    private LocalDateTime createdAt;
}
