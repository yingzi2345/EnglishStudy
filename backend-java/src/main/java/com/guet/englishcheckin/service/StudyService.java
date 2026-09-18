package com.guet.englishcheckin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guet.englishcheckin.common.BusinessException;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.entity.Word;
import com.guet.englishcheckin.entity.WordProgress;
import com.guet.englishcheckin.mapper.UserMapper;
import com.guet.englishcheckin.mapper.WordMapper;
import com.guet.englishcheckin.mapper.WordProgressMapper;
import com.guet.englishcheckin.util.TimeUtil;
import com.guet.englishcheckin.vo.QuizQuestionVO;
import com.guet.englishcheckin.vo.QuizResultVO;
import com.guet.englishcheckin.vo.StudyTaskVO;
import com.guet.englishcheckin.vo.WordProgressVO;
import com.guet.englishcheckin.vo.WordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学习系统服务：卡片式学习、艾宾浩斯复习、测验、错词本
 */
@Service
@RequiredArgsConstructor
public class StudyService {

    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final UserMapper userMapper;

    private final Random random = new Random();

    /** 艾宾浩斯间隔天数（索引 = interval_level） */
    private static final int[] INTERVAL_DAYS = {1, 2, 4, 7, 15, 30};

    // ══════════════════════════════════════════════
    //  今日学习任务
    // ══════════════════════════════════════════════

    /**
     * 获取今日学习任务：待复习词 + 新词
     * @param reviewLimit 复习词数量
     * @param newLimit    新词数量
     */
    public Map<String, Object> getTodayTasks(Long userId, int reviewLimit, int newLimit) {
        LocalDateTime now = TimeUtil.nowUtc();
        LocalDateTime todayEnd = now.toLocalDate().atTime(23, 59, 59);

        // 1. 待复习：next_review_at <= 今天结束，已学未掌握
        List<WordProgress> dueReviews = progressMapper.selectList(
                new LambdaQueryWrapper<WordProgress>()
                        .eq(WordProgress::getUserId, userId)
                        .eq(WordProgress::getIsLearned, 1)
                        .eq(WordProgress::getIsMastered, 0)
                        .isNotNull(WordProgress::getNextReviewAt)
                        .le(WordProgress::getNextReviewAt, todayEnd)
                        .orderByAsc(WordProgress::getNextReviewAt)
                        .last("LIMIT " + Math.max(reviewLimit, 0)));

        // 2. 新词：用户没有 progress 记录的词
        Set<Long> learnedIds = progressMapper.selectList(
                        new LambdaQueryWrapper<WordProgress>()
                                .eq(WordProgress::getUserId, userId)
                                .select(WordProgress::getWordId))
                .stream().map(WordProgress::getWordId).collect(Collectors.toSet());

        List<Word> allWords = wordMapper.selectList(null);
        List<Word> newWords = allWords.stream()
                .filter(w -> !learnedIds.contains(w.getId()))
                .collect(Collectors.toList());
        Collections.shuffle(newWords, random);
        int newCount = Math.min(newLimit, newWords.size());
        List<Word> pickedNew = newWords.subList(0, newCount);

        // 组装结果
        List<StudyTaskVO> reviewTasks = dueReviews.stream()
                .map(p -> toStudyTask(p.getWordId(), "review"))
                .collect(Collectors.toList());
        List<StudyTaskVO> newTasks = pickedNew.stream()
                .map(w -> toStudyTask(w.getId(), "new"))
                .collect(Collectors.toList());

        // 混合：复习在前，新词在后
        List<StudyTaskVO> all = new ArrayList<>();
        all.addAll(reviewTasks);
        all.addAll(newTasks);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("review_count", dueReviews.size());
        data.put("new_count", newCount);
        data.put("total", all.size());
        data.put("tasks", all);
        return data;
    }

    // ══════════════════════════════════════════════
    //  提交学习结果（艾宾浩斯算法）
    // ══════════════════════════════════════════════

    /**
     * 提交单个单词的学习结果
     * @param known true=认识，false=不认识
     */
    @Transactional
    public Map<String, Object> submitStudyResult(Long userId, Long wordId, boolean known) {
        if (wordMapper.selectById(wordId) == null) {
            throw new BusinessException(404, "单词不存在");
        }
        LocalDateTime now = TimeUtil.nowUtc();

        WordProgress progress = progressMapper.selectOne(
                new LambdaQueryWrapper<WordProgress>()
                        .eq(WordProgress::getUserId, userId)
                        .eq(WordProgress::getWordId, wordId));

        boolean created = false;
        if (progress == null) {
            progress = new WordProgress();
            progress.setUserId(userId);
            progress.setWordId(wordId);
            progress.setIsLearned(0);
            progress.setIsMastered(0);
            progress.setReviewCount(0);
            progress.setIntervalLevel(0);
            progress.setWrongCount(0);
            progress.setIsWrong(0);
            progress.setCreatedAt(now);
            created = true;
        }

        progress.setLastStudyAt(now);
        progress.setReviewCount((progress.getReviewCount() == null ? 0 : progress.getReviewCount()) + 1);

        int level = progress.getIntervalLevel() == null ? 0 : progress.getIntervalLevel();

        if (known) {
            // 认识：等级+1，计算下次复习时间
            level = Math.min(level + 1, INTERVAL_DAYS.length - 1);
            progress.setIntervalLevel(level);
            progress.setNextReviewAt(now.plusDays(INTERVAL_DAYS[level]));
            progress.setIsLearned(1);
            if (progress.getLearnedAt() == null) {
                progress.setLearnedAt(now);
            }
            // 达到最高等级标记为已掌握
            if (level == INTERVAL_DAYS.length - 1) {
                progress.setIsMastered(1);
            }
        } else {
            // 不认识：重置等级，加入错词本，明天复习
            level = 0;
            progress.setIntervalLevel(0);
            progress.setNextReviewAt(now.plusDays(1));
            progress.setWrongCount((progress.getWrongCount() == null ? 0 : progress.getWrongCount()) + 1);
            progress.setIsWrong(1);
            progress.setIsLearned(1);
            if (progress.getLearnedAt() == null) {
                progress.setLearnedAt(now);
            }
        }

        progress.setUpdatedAt(now);

        if (created) {
            progressMapper.insert(progress);
            // 刷新用户累计单词数
            refreshUserTotalWords(userId);
        } else {
            progressMapper.updateById(progress);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("word_id", wordId);
        data.put("known", known);
        data.put("interval_level", progress.getIntervalLevel());
        data.put("next_review_at", progress.getNextReviewAt());
        data.put("is_mastered", progress.getIsMastered() == 1);
        data.put("is_wrong", progress.getIsWrong() == 1);
        return data;
    }

    // ══════════════════════════════════════════════
    //  测验
    // ══════════════════════════════════════════════

    /**
     * 生成测验题目（从用户已学词中选）
     * 三种题型：listening(听音选词)、meaning(看词选义)、spelling(看义拼写)
     */
    public List<QuizQuestionVO> getQuizQuestions(Long userId, int count) {
        // 从用户已学词中选
        List<WordProgress> learned = progressMapper.selectList(
                new LambdaQueryWrapper<WordProgress>()
                        .eq(WordProgress::getUserId, userId)
                        .eq(WordProgress::getIsLearned, 1)
                        .orderByDesc(WordProgress::getLastStudyAt)
                        .last("LIMIT 100"));

        if (learned.isEmpty()) {
            throw new BusinessException(400, "还没有学习过单词，先去学习吧");
        }

        Collections.shuffle(learned, random);
        List<WordProgress> picked = learned.subList(0, Math.min(count, learned.size()));

        // 所有词用于生成干扰项
        List<Word> allWords = wordMapper.selectList(null);

        List<QuizQuestionVO> questions = new ArrayList<>();
        String[] types = {"listening", "meaning", "spelling"};

        for (int i = 0; i < picked.size(); i++) {
            Word word = wordMapper.selectById(picked.get(i).getWordId());
            if (word == null) continue;

            QuizQuestionVO q = new QuizQuestionVO();
            q.setWordId(word.getId());
            q.setWord(word.getWord());
            q.setPhonetic(word.getPhonetic());
            q.setMeaning(word.getMeaning());
            q.setAudioUrl(word.getAudioUrl());
            // 循环选题型
            q.setQuestionType(types[i % types.length]);

            // 生成4个选项（拼写题不需要选项）
            if (!"spelling".equals(q.getQuestionType())) {
                List<String> options = buildOptions(word, allWords, q.getQuestionType());
                q.setOptions(options);
            }

            questions.add(q);
        }
        return questions;
    }

    /**
     * 提交测验答案，判分并更新错词本
     */
    @Transactional
    public QuizResultVO submitQuiz(Long userId, List<Map<String, Object>> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new BusinessException(400, "答案不能为空");
        }

        int correct = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        List<Long> wrongWordIds = new ArrayList<>();
        LocalDateTime now = TimeUtil.nowUtc();

        for (Map<String, Object> ans : answers) {
            Long wordId = ans.get("word_id") == null ? null : Long.valueOf(ans.get("word_id").toString());
            String userAnswer = ans.get("user_answer") == null ? "" : ans.get("user_answer").toString().trim().toLowerCase();
            String questionType = ans.get("question_type") == null ? "" : ans.get("question_type").toString();

            Word word = wordMapper.selectById(wordId);
            if (word == null) continue;

            boolean isCorrect;
            if ("spelling".equals(questionType)) {
                // 拼写题：比对单词（忽略大小写）
                isCorrect = userAnswer.equalsIgnoreCase(word.getWord());
            } else {
                // 选择题：比对释义或单词
                String correctAnswer = "meaning".equals(questionType) ? word.getMeaning() : word.getWord();
                isCorrect = userAnswer.equals(correctAnswer);
            }

            if (isCorrect) {
                correct++;
            } else {
                wrongWordIds.add(wordId);
            }

            // 更新 progress
            WordProgress progress = progressMapper.selectOne(
                    new LambdaQueryWrapper<WordProgress>()
                            .eq(WordProgress::getUserId, userId)
                            .eq(WordProgress::getWordId, wordId));
            if (progress != null) {
                progress.setLastStudyAt(now);
                if (isCorrect) {
                    int level = progress.getIntervalLevel() == null ? 0 : progress.getIntervalLevel();
                    level = Math.min(level + 1, INTERVAL_DAYS.length - 1);
                    progress.setIntervalLevel(level);
                    progress.setNextReviewAt(now.plusDays(INTERVAL_DAYS[level]));
                    if (level == INTERVAL_DAYS.length - 1) {
                        progress.setIsMastered(1);
                    }
                } else {
                    progress.setIntervalLevel(0);
                    progress.setNextReviewAt(now.plusDays(1));
                    progress.setWrongCount((progress.getWrongCount() == null ? 0 : progress.getWrongCount()) + 1);
                    progress.setIsWrong(1);
                }
                progress.setUpdatedAt(now);
                progressMapper.updateById(progress);
            }

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("word_id", wordId);
            detail.put("word", word.getWord());
            detail.put("correct", isCorrect);
            detail.put("user_answer", userAnswer);
            detail.put("correct_answer", "spelling".equals(questionType) ? word.getWord() :
                    ("meaning".equals(questionType) ? word.getMeaning() : word.getWord()));
            details.add(detail);
        }

        QuizResultVO result = new QuizResultVO();
        result.setTotal(answers.size());
        result.setCorrect(correct);
        result.setWrong(answers.size() - correct);
        result.setScore((int) Math.round(correct * 100.0 / answers.size()));
        result.setDetails(details);
        result.setWrongWordIds(wrongWordIds);
        return result;
    }

    // ══════════════════════════════════════════════
    //  错词本
    // ══════════════════════════════════════════════

    /** 错词本列表 */
    public Map<String, Object> getWrongWords(Long userId, long page, long pageSize) {
        Page<WordProgress> pageResult = progressMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<WordProgress>()
                        .eq(WordProgress::getUserId, userId)
                        .eq(WordProgress::getIsWrong, 1)
                        .orderByDesc(WordProgress::getWrongCount));

        List<WordProgressVO> list = pageResult.getRecords().stream()
                .map(this::toProgressVO)
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", pageResult.getTotal());
        data.put("results", list);
        return data;
    }

    /** 从错词本移除（答对后手动移除） */
    @Transactional
    public Map<String, Object> removeWrongWord(Long userId, Long wordId) {
        WordProgress progress = progressMapper.selectOne(
                new LambdaQueryWrapper<WordProgress>()
                        .eq(WordProgress::getUserId, userId)
                        .eq(WordProgress::getWordId, wordId));
        if (progress == null) {
            throw new BusinessException(404, "未找到学习记录");
        }
        progress.setIsWrong(0);
        progress.setUpdatedAt(TimeUtil.nowUtc());
        progressMapper.updateById(progress);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("word_id", wordId);
        data.put("is_wrong", false);
        return data;
    }

    /** 今日待复习数量（首页展示用） */
    public Map<String, Object> getTodayStats(Long userId) {
        LocalDateTime now = TimeUtil.nowUtc();
        LocalDateTime todayEnd = now.toLocalDate().atTime(23, 59, 59);

        long dueReview = progressMapper.selectCount(
                new LambdaQueryWrapper<WordProgress>()
                        .eq(WordProgress::getUserId, userId)
                        .eq(WordProgress::getIsLearned, 1)
                        .eq(WordProgress::getIsMastered, 0)
                        .isNotNull(WordProgress::getNextReviewAt)
                        .le(WordProgress::getNextReviewAt, todayEnd));

        long wrongCount = progressMapper.selectCount(
                new LambdaQueryWrapper<WordProgress>()
                        .eq(WordProgress::getUserId, userId)
                        .eq(WordProgress::getIsWrong, 1));

        long mastered = progressMapper.selectCount(
                new LambdaQueryWrapper<WordProgress>()
                        .eq(WordProgress::getUserId, userId)
                        .eq(WordProgress::getIsMastered, 1));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("due_review", dueReview);
        data.put("wrong_count", wrongCount);
        data.put("mastered_count", mastered);
        return data;
    }

    // ══════════════════════════════════════════════
    //  私有辅助
    // ══════════════════════════════════════════════

    private StudyTaskVO toStudyTask(Long wordId, String type) {
        Word word = wordMapper.selectById(wordId);
        StudyTaskVO vo = new StudyTaskVO();
        vo.setWordId(wordId);
        vo.setTaskType(type);
        if (word != null) {
            vo.setWord(word.getWord());
            vo.setPhonetic(word.getPhonetic());
            vo.setMeaning(word.getMeaning());
            vo.setExampleEn(word.getExampleEn());
            vo.setExampleZh(word.getExampleZh());
            vo.setAudioUrl(word.getAudioUrl());
        }
        return vo;
    }

    /** 生成4个选项，正确答案随机位置 */
    private List<String> buildOptions(Word correctWord, List<Word> allWords, String type) {
        Set<String> opts = new HashSet<>();
        String correct = "meaning".equals(type) ? correctWord.getMeaning() : correctWord.getWord();
        opts.add(correct);

        List<Word> shuffled = new ArrayList<>(allWords);
        Collections.shuffle(shuffled, random);

        for (Word w : shuffled) {
            if (opts.size() >= 4) break;
            String val = "meaning".equals(type) ? w.getMeaning() : w.getWord();
            if (!val.equals(correct) && val != null && !val.isBlank()) {
                opts.add(val);
            }
        }

        List<String> result = new ArrayList<>(opts);
        Collections.shuffle(result, random);
        return result;
    }

    private void refreshUserTotalWords(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            long learnedCount = progressMapper.selectCount(
                    new LambdaQueryWrapper<WordProgress>()
                            .eq(WordProgress::getUserId, userId)
                            .eq(WordProgress::getIsLearned, 1));
            user.setTotalWords((int) learnedCount);
            userMapper.updateById(user);
        }
    }

    private WordProgressVO toProgressVO(WordProgress p) {
        WordProgressVO vo = new WordProgressVO();
        vo.setId(p.getId());
        vo.setUser(p.getUserId());
        vo.setWord(p.getWordId());
        vo.setIsLearned(p.getIsLearned());
        vo.setIsMastered(p.getIsMastered());
        vo.setLearnedAt(p.getLearnedAt());
        vo.setReviewCount(p.getReviewCount());
        vo.setWrongCount(p.getWrongCount());
        vo.setIsWrong(p.getIsWrong());
        vo.setIntervalLevel(p.getIntervalLevel());
        vo.setCreatedAt(p.getCreatedAt());
        vo.setUpdatedAt(p.getUpdatedAt());
        Word word = wordMapper.selectById(p.getWordId());
        if (word != null) {
            vo.setWordName(word.getWord());
            vo.setWordMeaning(word.getMeaning());
        }
        return vo;
    }
}
