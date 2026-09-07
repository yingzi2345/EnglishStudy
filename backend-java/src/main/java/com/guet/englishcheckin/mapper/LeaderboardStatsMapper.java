package com.guet.englishcheckin.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 排行榜统计查询
 * 日/周/月榜：基于 WordProgress 实际学习数据（is_learned=1）按时间段聚合；
 * days_count 使用 MySQL DATE(learned_at) 去重统计（避开 ORM 日期函数时区转换问题，与 Django 侧 RawSQL 一致）。
 */
public interface LeaderboardStatsMapper {

    /**
     * 时间段内 TOP 50 用户学习统计（可带结束时间）
     */
    @Select("<script>" +
            "SELECT wp.user_id, u.nickname, u.avatar_url, u.total_days, u.max_continuous, " +
            "       COUNT(*) AS words_count, COUNT(DISTINCT DATE(wp.learned_at)) AS days_count " +
            "FROM tb_word_progress wp " +
            "JOIN tb_user u ON wp.user_id = u.id " +
            "WHERE wp.is_learned = 1 AND wp.learned_at &gt;= #{start} " +
            "<if test='end != null'> AND wp.learned_at &lt; #{end}</if> " +
            "GROUP BY wp.user_id, u.nickname, u.avatar_url, u.total_days, u.max_continuous " +
            "ORDER BY words_count DESC, days_count DESC " +
            "LIMIT 50" +
            "</script>")
    List<Map<String, Object>> selectPeriodStats(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    /**
     * 当前用户在某时间段内的学习统计（用于 my_rank）
     */
    @Select("<script>" +
            "SELECT COUNT(*) AS words_count, COUNT(DISTINCT DATE(learned_at)) AS days_count " +
            "FROM tb_word_progress " +
            "WHERE user_id = #{userId} AND is_learned = 1 AND learned_at &gt;= #{start} " +
            "<if test='end != null'> AND learned_at &lt; #{end}</if>" +
            "</script>")
    Map<String, Object> selectUserPeriodStats(@Param("userId") Long userId,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    /**
     * 首页统计数据：时间段内学习单词总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM tb_word_progress " +
            "WHERE is_learned = 1 AND learned_at &gt;= #{start} " +
            "<if test='end != null'> AND learned_at &lt; #{end}</if>" +
            "</script>")
    Long countLearnedWords(@Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);
}
