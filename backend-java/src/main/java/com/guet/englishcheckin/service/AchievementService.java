package com.guet.englishcheckin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guet.englishcheckin.entity.Achievement;
import com.guet.englishcheckin.entity.Checkin;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.entity.UserAchievement;
import com.guet.englishcheckin.mapper.AchievementMapper;
import com.guet.englishcheckin.mapper.CheckinMapper;
import com.guet.englishcheckin.mapper.UserAchievementMapper;
import com.guet.englishcheckin.mapper.UserMapper;
import com.guet.englishcheckin.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成就徽章服务
 */
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementMapper achievementMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final UserMapper userMapper;
    private final CheckinMapper checkinMapper;

    /**
     * 获取用户徽章列表（含未解锁），并自动检查解锁新徽章
     */
    @Transactional
    public List<Map<String, Object>> getUserAchievements(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return List.of();
        }

        // 用户当前数据
        int totalWords = user.getTotalWords() == null ? 0 : user.getTotalWords();
        int totalDays = user.getTotalDays() == null ? 0 : user.getTotalDays();
        int continuousDays = calcContinuousDays(userId);

        // 已解锁的徽章 ID
        Set<Long> unlockedIds = userAchievementMapper.selectList(
                        new LambdaQueryWrapper<UserAchievement>().eq(UserAchievement::getUserId, userId))
                .stream().map(UserAchievement::getAchievementId).collect(Collectors.toSet());

        // 所有徽章
        List<Achievement> all = achievementMapper.selectList(
                new LambdaQueryWrapper<Achievement>().orderByAsc(Achievement::getSortOrder));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Achievement a : all) {
            boolean unlocked = unlockedIds.contains(a.getId());
            int progress = calcProgress(a, totalWords, totalDays, continuousDays);

            // 未解锁但已达标 → 自动解锁
            if (!unlocked && progress >= a.getConditionValue()) {
                UserAchievement ua = new UserAchievement();
                ua.setUserId(userId);
                ua.setAchievementId(a.getId());
                ua.setUnlockedAt(TimeUtil.nowUtc());
                userAchievementMapper.insert(ua);
                unlocked = true;
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("name", a.getName());
            m.put("description", a.getDescription());
            m.put("icon", a.getIcon());
            m.put("condition_type", a.getConditionType());
            m.put("condition_value", a.getConditionValue());
            m.put("progress", Math.min(progress, a.getConditionValue()));
            m.put("unlocked", unlocked);
            result.add(m);
        }
        return result;
    }

    /** 计算某徽章的当前进度值 */
    private int calcProgress(Achievement a, int totalWords, int totalDays, int continuousDays) {
        return switch (a.getConditionType()) {
            case "total_words" -> totalWords;
            case "total_days" -> totalDays;
            case "continuous_days" -> continuousDays;
            default -> 0;
        };
    }

    /** 连续打卡天数 */
    private int calcContinuousDays(Long userId) {
        List<Checkin> checkins = checkinMapper.selectList(
                new LambdaQueryWrapper<Checkin>().eq(Checkin::getUserId, userId));
        Set<LocalDate> dates = new HashSet<>();
        for (Checkin c : checkins) {
            if (c.getCheckinDate() != null) {
                dates.add(c.getCheckinDate());
            }
        }
        LocalDate today = TimeUtil.todayShanghai();
        int continuous = 0;
        for (int i = 0; i < 365; i++) {
            if (dates.contains(today.minusDays(i))) {
                continuous++;
            } else {
                break;
            }
        }
        return continuous;
    }
}
