package com.guet.englishcheckin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guet.englishcheckin.entity.Checkin;
import com.guet.englishcheckin.entity.WordProgress;
import com.guet.englishcheckin.mapper.CheckinMapper;
import com.guet.englishcheckin.mapper.WordProgressMapper;
import com.guet.englishcheckin.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学习数据统计服务：30 天打卡热力图 + 每日学习趋势
 * 数据来源：tb_checkin（打卡，checkin_date 为东八区日期）、tb_word_progress（学习，learned_at 为 UTC）
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final CheckinMapper checkinMapper;
    private final WordProgressMapper progressMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 统计窗口天数 */
    private static final int DAYS = 30;

    /**
     * 最近 30 天学习数据可视化
     * heatmap: [{date, checked, word_count, study_duration, continuous_days}]（按日期升序，含今天）
     * trend:   [{date, words}]（每日学习单词数，按东八区日期归并 learned_at）
     */
    public Map<String, Object> analytics(Long userId) {
        LocalDate today = TimeUtil.todayShanghai();
        LocalDate start = today.minusDays(DAYS - 1L);

        // ── 热力图：最近 30 天打卡记录 ──
        Map<LocalDate, Checkin> checkinByDate = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                        .eq(Checkin::getUserId, userId)
                        .ge(Checkin::getCheckinDate, start)
                        .le(Checkin::getCheckinDate, today))
                .stream()
                .collect(Collectors.toMap(Checkin::getCheckinDate, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> heatmap = new ArrayList<>(DAYS);
        for (int i = 0; i < DAYS; i++) {
            LocalDate d = start.plusDays(i);
            Checkin c = checkinByDate.get(d);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.format(DATE_FMT));
            m.put("checked", c != null);
            m.put("word_count", c == null || c.getWordCount() == null ? 0 : c.getWordCount());
            m.put("study_duration", c == null || c.getStudyDuration() == null ? 0 : c.getStudyDuration());
            m.put("continuous_days", c == null || c.getContinuousDays() == null ? 0 : c.getContinuousDays());
            heatmap.add(m);
        }

        // ── 学习趋势：最近 30 天每日学习单词数（learned_at 存 UTC，转东八区日期归并） ──
        LocalDateTime utcStart = TimeUtil.shanghaiMidnightToUtc(start);
        List<WordProgress> progresses = progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getUserId, userId)
                .eq(WordProgress::getIsLearned, 1)
                .ge(WordProgress::getLearnedAt, utcStart)
                .select(WordProgress::getLearnedAt));

        Map<LocalDate, Integer> wordsByDay = new HashMap<>();
        for (WordProgress p : progresses) {
            if (p.getLearnedAt() == null) {
                continue;
            }
            LocalDate day = p.getLearnedAt().atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(TimeUtil.SHANGHAI)
                    .toLocalDate();
            wordsByDay.merge(day, 1, Integer::sum);
        }

        List<Map<String, Object>> trend = new ArrayList<>(DAYS);
        for (int i = 0; i < DAYS; i++) {
            LocalDate d = start.plusDays(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.format(DATE_FMT));
            m.put("words", wordsByDay.getOrDefault(d, 0));
            trend.add(m);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("heatmap", heatmap);
        data.put("trend", trend);
        return data;
    }
}
