package com.guet.englishcheckin.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打卡记录（附带用户昵称），对应 Django CheckinSerializer
 */
@Data
public class CheckinVO {

    private Long id;
    private Long user;
    private String nickname;
    private LocalDate checkinDate;
    private LocalDateTime checkinTime;
    private Integer wordCount;
    private Integer studyDuration;
    private Integer continuousDays;
    private String note;
}
