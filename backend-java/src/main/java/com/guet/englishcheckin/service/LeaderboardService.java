package com.guet.englishcheckin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guet.englishcheckin.entity.Checkin;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.mapper.CheckinMapper;
import com.guet.englishcheckin.mapper.LeaderboardStatsMapper;
import com.guet.englishcheckin.mapper.UserMapper;
import com.guet.englishcheckin.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 排行榜服务（对应 Django LeaderboardViewSet）：
 * 日/周/月榜基于 WordProgress 实际学习数据聚合，总榜基于 User.total_words；
 * 时间窗口统一使用东八区 0 点换算的 UTC 时刻（与 Django timezone-aware 语义一致）。
 */
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardStatsMapper statsMapper;
    private final UserMapper userMapper;
    private final CheckinMapper checkinMapper;

    /**
     * 排行榜（daily / weekly / monthly / alltime）
     */
    public Map<String, Object> rank(String period, Long userId) {
        List<Map<String, Object>> list;
        if ("alltime".equals(period)) {
            list = alltimeRank();
        } else {
            PeriodWindow window = periodWindow(period);
            list = periodRank(window);
        }

        // 当前用户排名
        Map<String, Object> myRank = null;
        for (Map<String, Object> item : list) {
            if (item.get("user_id") != null && item.get("user_id").equals(userId)) {
                myRank = item;
                break;
            }
        }
        if (myRank == null && !"alltime".equals(period)) {
            PeriodWindow window = periodWindow(period);
            Map<String, Object> stats = statsMapper.selectUserPeriodStats(userId, window.start(), window.end());
            long wordsCount = toLong(stats.get("words_count"));
            if (wordsCount > 0) {
                myRank = new LinkedHashMap<>();
                User user = userMapper.selectById(userId);
                if (user != null) {
                    myRank.put("rank", null);
                    myRank.put("user_id", user.getId());
                    myRank.put("nickname", user.getNickname());
                    myRank.put("avatar_url", user.getAvatarUrl());
                    myRank.put("days_count", toLong(stats.get("days_count")));
                    myRank.put("words_count", wordsCount);
                    myRank.put("max_continuous", user.getMaxContinuous());
                }
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("period", period);
        data.put("list", list);
        data.put("my_rank", myRank);
        return data;
    }

    /**
     * 首页统计（对应 Django LeaderboardViewSet.stats）
     */
    public Map<String, Object> stats() {
        LocalDate today = TimeUtil.todayShanghai();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        LocalDate monthStart = today.withDayOfMonth(1);

        LocalDateTime todayStart = TimeUtil.shanghaiMidnightToUtc(today);
        LocalDateTime tomorrowStart = TimeUtil.shanghaiMidnightToUtc(today.plusDays(1));
        LocalDateTime weekStartUtc = TimeUtil.shanghaiMidnightToUtc(weekStart);
        LocalDateTime monthStartUtc = TimeUtil.shanghaiMidnightToUtc(monthStart);

        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, 1));
        long todayCheckins = checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getCheckinDate, today));
        long todayWords = statsMapper.countLearnedWords(todayStart, tomorrowStart);
        long weeklyWords = statsMapper.countLearnedWords(weekStartUtc, null);
        long monthlyWords = statsMapper.countLearnedWords(monthStartUtc, null);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total_users", totalUsers);
        data.put("today_checkin_count", todayCheckins);
        data.put("today_words", todayWords);
        data.put("weekly_words", weeklyWords);
        data.put("monthly_words", monthlyWords);
        return data;
    }

    // ──── 私有方法 ────

    private record PeriodWindow(LocalDateTime start, LocalDateTime end) {
    }

    private PeriodWindow periodWindow(String period) {
        LocalDate today = TimeUtil.todayShanghai();
        return switch (period) {
            case "daily" -> new PeriodWindow(
                    TimeUtil.shanghaiMidnightToUtc(today),
                    TimeUtil.shanghaiMidnightToUtc(today.plusDays(1)));
            case "weekly" -> {
                LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
                yield new PeriodWindow(TimeUtil.shanghaiMidnightToUtc(weekStart), null);
            }
            case "monthly" -> {
                LocalDate monthStart = today.withDayOfMonth(1);
                yield new PeriodWindow(TimeUtil.shanghaiMidnightToUtc(monthStart), null);
            }
            default -> throw new IllegalArgumentException("不支持的排行周期: " + period);
        };
    }

    private List<Map<String, Object>> periodRank(PeriodWindow window) {
        List<Map<String, Object>> rows = statsMapper.selectPeriodStats(window.start(), window.end());
        List<Map<String, Object>> list = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("user_id", toLong(row.get("user_id")));
            item.put("nickname", row.get("nickname"));
            item.put("avatar_url", row.get("avatar_url"));
            item.put("days_count", toLong(row.get("days_count")));
            item.put("words_count", toLong(row.get("words_count")));
            item.put("max_continuous", toLong(row.get("max_continuous")));
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> alltimeRank() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .gt(User::getTotalWords, 0)
                .orderByDesc(User::getTotalWords)
                .last("LIMIT 50"));
        List<Map<String, Object>> list = new ArrayList<>();
        int rank = 1;
        for (User u : users) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("user_id", u.getId());
            item.put("nickname", u.getNickname());
            item.put("avatar_url", u.getAvatarUrl());
            item.put("days_count", u.getTotalDays());
            item.put("words_count", u.getTotalWords());
            item.put("max_continuous", u.getMaxContinuous());
            list.add(item);
        }
        return list;
    }

    private long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(o.toString());
    }
}
