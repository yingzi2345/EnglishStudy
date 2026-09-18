package com.guet.englishcheckin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guet.englishcheckin.entity.Word;
import com.guet.englishcheckin.mapper.WordMapper;
import com.guet.englishcheckin.vo.QuizQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 词汇量测试服务
 * 按难度分层抽题，根据答对情况估算词汇量
 */
@Service
@RequiredArgsConstructor
public class VocabTestService {

    private final WordMapper wordMapper;
    private final Random random = new Random();

    /**
     * 生成词汇量测试题目（按难度 1/2/3 分层，默认各 10 题共 30 题）
     */
    public List<QuizQuestionVO> getQuestions(int count) {
        int perLevel = Math.max(count / 3, 1);
        List<Word> allWords = wordMapper.selectList(null);

        // 按难度分组
        Map<Integer, List<Word>> byLevel = allWords.stream()
                .filter(w -> w.getLevel() != null)
                .collect(Collectors.groupingBy(Word::getLevel));

        List<QuizQuestionVO> questions = new ArrayList<>();
        for (int level = 1; level <= 3; level++) {
            List<Word> pool = byLevel.getOrDefault(level, new ArrayList<>());
            if (pool.isEmpty()) continue;
            Collections.shuffle(pool, random);
            int pick = Math.min(perLevel, pool.size());
            for (int i = 0; i < pick; i++) {
                Word w = pool.get(i);
                QuizQuestionVO q = new QuizQuestionVO();
                q.setWordId(w.getId());
                q.setWord(w.getWord());
                q.setPhonetic(w.getPhonetic());
                q.setMeaning(w.getMeaning());
                q.setAudioUrl(w.getAudioUrl());
                q.setQuestionType("meaning"); // 看词选义
                q.setOptions(buildOptions(w, allWords));
                questions.add(q);
            }
        }
        Collections.shuffle(questions, random);
        return questions;
    }

    /**
     * 提交答案，估算词汇量
     */
    public Map<String, Object> submitAnswers(List<Map<String, Object>> answers) {
        if (answers == null || answers.isEmpty()) {
            return Map.of("vocab_size", 0, "level", "未知", "correct", 0, "total", 0);
        }

        // 按难度统计答对
        Map<Integer, int[]> levelStats = new HashMap<>(); // level -> [correct, total]
        int totalCorrect = 0;

        for (Map<String, Object> ans : answers) {
            Long wordId = ans.get("word_id") == null ? null : Long.valueOf(ans.get("word_id").toString());
            String userAnswer = ans.get("user_answer") == null ? "" : ans.get("user_answer").toString();
            Word word = wordMapper.selectById(wordId);
            if (word == null) continue;

            int level = word.getLevel() == null ? 1 : word.getLevel();
            boolean correct = userAnswer.equals(word.getMeaning());
            if (correct) totalCorrect++;

            int[] stats = levelStats.computeIfAbsent(level, k -> new int[]{0, 0});
            stats[1]++;
            if (correct) stats[0]++;
        }

        // 词汇量估算：level1 上限2000, level2 上限3000, level3 上限5000
        int[] levelMax = {0, 2000, 3000, 5000};
        int vocabSize = 0;
        for (int level = 1; level <= 3; level++) {
            int[] stats = levelStats.get(level);
            if (stats != null && stats[1] > 0) {
                vocabSize += (int) (levelMax[level] * (double) stats[0] / stats[1]);
            }
        }
        vocabSize = Math.max(500, Math.min(vocabSize, 10000));

        // 等级评价
        String level = evaluateLevel(vocabSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vocab_size", vocabSize);
        result.put("level", level);
        result.put("correct", totalCorrect);
        result.put("total", answers.size());
        result.put("accuracy", (int) Math.round(totalCorrect * 100.0 / answers.size()));
        return result;
    }

    private String evaluateLevel(int vocab) {
        if (vocab < 1500) return "入门级";
        if (vocab < 3000) return "初级（初中水平）";
        if (vocab < 4500) return "中级（高中/CET-4）";
        if (vocab < 6500) return "中高级（CET-6）";
        if (vocab < 8500) return "高级（专八/考研）";
        return "专家级（GRE/托福）";
    }

    /** 生成4个选项（看词选义） */
    private List<String> buildOptions(Word correctWord, List<Word> allWords) {
        Set<String> opts = new HashSet<>();
        opts.add(correctWord.getMeaning());
        List<Word> shuffled = new ArrayList<>(allWords);
        Collections.shuffle(shuffled, random);
        for (Word w : shuffled) {
            if (opts.size() >= 4) break;
            if (w.getMeaning() != null && !w.getMeaning().isBlank()
                    && !w.getMeaning().equals(correctWord.getMeaning())) {
                opts.add(w.getMeaning());
            }
        }
        List<String> result = new ArrayList<>(opts);
        Collections.shuffle(result, random);
        return result;
    }
}
