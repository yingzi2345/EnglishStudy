package com.guet.englishcheckin.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * 时间工具：与 Django 侧时区语义对齐
 * - 数据库时间字段统一存 UTC（Django USE_TZ=True 行为）
 * - 打卡日期 checkin_date 使用东八区日期（Django date.today() 行为）
 */
public final class TimeUtil {

    public static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private TimeUtil() {
    }

    /** 当前 UTC 时间（写入/比较 learned_at、checkin_time 等） */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(Clock.systemUTC());
    }

    /** 东八区今天（打卡日期语义） */
    public static LocalDate todayShanghai() {
        return LocalDate.now(SHANGHAI);
    }

    /** 东八区某日 00:00 对应的 UTC 时刻（排行榜窗口起点） */
    public static LocalDateTime shanghaiMidnightToUtc(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.MIN, SHANGHAI)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }
}
