package com.guet.englishcheckin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guet.englishcheckin.common.BusinessException;
import com.guet.englishcheckin.dto.CheckinRequest;
import com.guet.englishcheckin.entity.Checkin;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.mapper.CheckinMapper;
import com.guet.englishcheckin.mapper.UserMapper;
import com.guet.englishcheckin.util.TimeUtil;
import com.guet.englishcheckin.vo.CheckinVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 打卡模块服务：每日打卡、打卡状态、日历、记录、连续打卡
 */
@Service
@RequiredArgsConstructor
public class CheckinService {

    private final CheckinMapper checkinMapper;
    private final UserMapper userMapper;
    private final UserService userService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 执行每日打卡（对应 Django CheckinViewSet.do_checkin）
     */
    @Transactional
    public Map<String, Object> doCheckin(Long userId, CheckinRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        LocalDate today = TimeUtil.todayShanghai();

        // 今日是否已打卡
        boolean already = checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .eq(Checkin::getCheckinDate, today)) > 0;
        if (already) {
            throw new BusinessException(400, "今日已打卡，请勿重复操作");
        }

        // 计算连续打卡天数：昨天打卡则 +1，否则从 1 开始
        int continuousDays = 1;
        Checkin yesterday = checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .eq(Checkin::getCheckinDate, today.minusDays(1)));
        if (yesterday != null) {
            continuousDays = (yesterday.getContinuousDays() == null ? 0 : yesterday.getContinuousDays()) + 1;
        }

        Checkin checkin = new Checkin();
        checkin.setUserId(userId);
        checkin.setCheckinDate(today);
        checkin.setCheckinTime(TimeUtil.nowUtc());
        checkin.setWordCount(req.getWordCount() == null ? 0 : req.getWordCount());
        checkin.setStudyDuration(req.getStudyDuration() == null ? 0 : req.getStudyDuration());
        checkin.setContinuousDays(continuousDays);
        checkin.setNote(req.getNote() == null ? "" : req.getNote());
        checkin.setCreatedAt(TimeUtil.nowUtc());
        checkinMapper.insert(checkin);

        // 更新用户统计数据
        long totalDays = checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId));
        user.setTotalDays((int) totalDays);
        if (continuousDays > (user.getMaxContinuous() == null ? 0 : user.getMaxContinuous())) {
            user.setMaxContinuous(continuousDays);
        }
        userMapper.updateById(user);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("checkin", toCheckinVO(checkin, user.getNickname()));
        data.put("continuous_days", continuousDays);
        data.put("total_days", user.getTotalDays());
        data.put("message", "打卡成功！已连续打卡" + continuousDays + "天");
        return data;
    }

    /**
     * 今日打卡状态（对应 Django today_status）
     */
    public Map<String, Object> todayStatus(Long userId) {
        LocalDate today = TimeUtil.todayShanghai();
        Checkin record = checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .eq(Checkin::getCheckinDate, today));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("is_checked", record != null);
        data.put("today_record", record == null ? null : toCheckinVO(record, null));
        return data;
    }

    /**
     * 打卡日历（对应 Django calendar）
     */
    public Map<String, Object> calendar(Long userId, Integer year, Integer month) {
        LocalDate now = TimeUtil.todayShanghai();
        int y = year == null ? now.getYear() : year;
        int m = month == null ? now.getMonthValue() : month;
        int days = YearMonth.of(y, m).lengthOfMonth();

        List<Checkin> checkins = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .ge(Checkin::getCheckinDate, LocalDate.of(y, m, 1))
                .le(Checkin::getCheckinDate, LocalDate.of(y, m, days)));

        List<String> checkinDates = checkins.stream()
                .map(c -> c.getCheckinDate().format(DATE_FMT))
                .collect(Collectors.toList());

        // 打卡详情（含连续天数/单词数），供前端日历火焰等级使用
        List<Map<String, Object>> checkinDetails = checkins.stream().map(c -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", c.getCheckinDate().format(DATE_FMT));
            item.put("continuous_days", c.getContinuousDays() == null ? 0 : c.getContinuousDays());
            item.put("word_count", c.getWordCount() == null ? 0 : c.getWordCount());
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("year", y);
        data.put("month", m);
        data.put("days", days);
        data.put("checkin_dates", checkinDates);
        data.put("checkin_details", checkinDetails);
        return data;
    }

    /**
     * 打卡记录列表（最近 90 条，对应 Django records）
     */
    public List<CheckinVO> records(Long userId) {
        User user = userMapper.selectById(userId);
        List<Checkin> list = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .orderByDesc(Checkin::getCheckinDate)
                .last("LIMIT 90"));
        String nickname = user == null ? null : user.getNickname();
        return list.stream().map(c -> toCheckinVO(c, nickname)).collect(Collectors.toList());
    }

    /**
     * 连续打卡信息（对应 Django streak）
     */
    public Map<String, Object> streak(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        LocalDate today = TimeUtil.todayShanghai();
        int continuous = userService.calcContinuousDays(userId, today);
        boolean isTodayChecked = checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .eq(Checkin::getCheckinDate, today)) > 0;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("continuous_days", continuous);
        data.put("max_continuous", user.getMaxContinuous());
        data.put("total_days", user.getTotalDays());
        data.put("is_today_checked", isTodayChecked);
        return data;
    }

    /**
     * 当前用户打卡列表（GET /api/checkin，DRF list 兼容）
     */
    public List<CheckinVO> listByUser(Long userId) {
        return records(userId);
    }

    private CheckinVO toCheckinVO(Checkin c, String nickname) {
        CheckinVO vo = new CheckinVO();
        vo.setId(c.getId());
        vo.setUser(c.getUserId());
        vo.setNickname(nickname);
        vo.setCheckinDate(c.getCheckinDate());
        vo.setCheckinTime(c.getCheckinTime());
        vo.setWordCount(c.getWordCount());
        vo.setStudyDuration(c.getStudyDuration());
        vo.setContinuousDays(c.getContinuousDays());
        vo.setNote(c.getNote());
        return vo;
    }
}
