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
import com.guet.englishcheckin.vo.WordProgressVO;
import com.guet.englishcheckin.vo.WordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 单词模块服务：单词列表/详情/搜索、每日推荐、学习进度管理
 */
@Service
@RequiredArgsConstructor
public class WordService {

    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final UserMapper userMapper;

    private final Random random = new Random();

    /**
     * 单词分页列表（支持 category 筛选与 search 关键词，对应 Django WordViewSet.list）
     */
    public Map<String, Object> list(String category, String search, long page, long pageSize) {
        LambdaQueryWrapper<Word> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq(Word::getCategory, category);
        }
        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like(Word::getWord, search).or().like(Word::getMeaning, search));
        }
        wrapper.orderByAsc(Word::getId);

        Page<Word> pageResult = wordMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<WordVO> results = pageResult.getRecords().stream().map(this::toWordVO).toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", pageResult.getTotal());
        data.put("next", null);
        data.put("previous", null);
        data.put("results", results);
        return data;
    }

    /**
     * 单词详情（附带当前用户学习状态）
     */
    public WordVO retrieve(Long id, Long userId) {
        Word word = wordMapper.selectById(id);
        if (word == null) {
            throw new BusinessException(404, "单词不存在");
        }
        WordVO vo = toWordVO(word);
        fillUserStatus(vo, userId);
        return vo;
    }

    public WordVO create(Word word) {
        if (word.getWord() == null || word.getWord().isBlank()) {
            throw new BusinessException(400, "单词内容不能为空");
        }
        if (word.getMeaning() == null || word.getMeaning().isBlank()) {
            throw new BusinessException(400, "单词释义不能为空");
        }
        LocalDateTime now = TimeUtil.nowUtc();
        word.setPhonetic(word.getPhonetic() == null ? "" : word.getPhonetic());
        word.setExampleEn(word.getExampleEn() == null ? "" : word.getExampleEn());
        word.setExampleZh(word.getExampleZh() == null ? "" : word.getExampleZh());
        word.setAudioUrl(word.getAudioUrl() == null ? "" : word.getAudioUrl());
        word.setLevel(word.getLevel() == null ? 1 : word.getLevel());
        word.setCategory(word.getCategory() == null ? "CET-4" : word.getCategory());
        word.setCreatedAt(now);
        word.setUpdatedAt(now);
        wordMapper.insert(word);
        return toWordVO(word);
    }

    public WordVO update(Long id, Word req) {
        Word word = wordMapper.selectById(id);
        if (word == null) {
            throw new BusinessException(404, "单词不存在");
        }
        if (req.getWord() != null) {
            word.setWord(req.getWord());
        }
        if (req.getPhonetic() != null) {
            word.setPhonetic(req.getPhonetic());
        }
        if (req.getMeaning() != null) {
            word.setMeaning(req.getMeaning());
        }
        if (req.getExampleEn() != null) {
            word.setExampleEn(req.getExampleEn());
        }
        if (req.getExampleZh() != null) {
            word.setExampleZh(req.getExampleZh());
        }
        if (req.getAudioUrl() != null) {
            word.setAudioUrl(req.getAudioUrl());
        }
        if (req.getLevel() != null) {
            word.setLevel(req.getLevel());
        }
        if (req.getCategory() != null) {
            word.setCategory(req.getCategory());
        }
        word.setUpdatedAt(TimeUtil.nowUtc());
        wordMapper.updateById(word);
        return toWordVO(word);
    }

    public void delete(Long id) {
        if (wordMapper.selectById(id) == null) {
            throw new BusinessException(404, "单词不存在");
        }
        wordMapper.deleteById(id);
    }

    /**
     * 所有单词分类
     */
    public List<String> categories() {
        return wordMapper.selectList(new LambdaQueryWrapper<Word>()
                        .select(Word::getCategory))
                .stream().map(Word::getCategory)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 随机获取一个单词（可筛选 level / category）
     */
    public WordVO randomWord(Integer level, String category, Long userId) {
        LambdaQueryWrapper<Word> wrapper = new LambdaQueryWrapper<>();
        if (level != null) {
            wrapper.eq(Word::getLevel, level);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Word::getCategory, category);
        }
        List<Long> ids = wordMapper.selectList(wrapper.select(Word::getId))
                .stream().map(Word::getId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            throw new BusinessException(404, "暂无单词数据");
        }
        Word word = wordMapper.selectById(ids.get(random.nextInt(ids.size())));
        WordVO vo = toWordVO(word);
        fillUserStatus(vo, userId);
        return vo;
    }

    /**
     * 每日推荐单词（默认 10 个，优先未学习，对应 Django daily_words）
     */
    public List<WordVO> dailyWords(int count, Long userId) {
        if (count <= 0) {
            count = 10;
        }
        long total = wordMapper.selectCount(null);
        if (total == 0) {
            throw new BusinessException(404, "暂无单词数据");
        }
        // 已学单词 id 集合
        Set<Long> learnedIds = progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
                        .eq(WordProgress::getUserId, userId)
                        .eq(WordProgress::getIsLearned, 1)
                        .select(WordProgress::getWordId))
                .stream().map(WordProgress::getWordId).collect(Collectors.toSet());

        List<Word> unlearned = wordMapper.selectList(new LambdaQueryWrapper<Word>()
                .notIn(!learnedIds.isEmpty(), Word::getId, learnedIds));
        List<Word> pool;
        if (unlearned.size() >= count) {
            pool = unlearned;
        } else {
            pool = wordMapper.selectList(null);
        }
        Collections.shuffle(pool, random);
        List<Word> picked = pool.subList(0, Math.min(count, pool.size()));
        List<WordVO> result = picked.stream().map(this::toWordVO).collect(Collectors.toList());
        result.forEach(vo -> fillUserStatus(vo, userId));
        return result;
    }

    /**
     * 标记单词为已学/已掌握（对应 Django mark_learned）
     */
    @Transactional
    public WordProgressVO markLearned(Long userId, Long wordId, Boolean isMastered) {
        if (wordMapper.selectById(wordId) == null) {
            throw new BusinessException(404, "单词不存在");
        }
        boolean mastered = Boolean.TRUE.equals(isMastered);
        LocalDateTime now = TimeUtil.nowUtc();

        WordProgress progress = progressMapper.selectOne(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getUserId, userId)
                .eq(WordProgress::getWordId, wordId));
        boolean created = false;
        if (progress == null) {
            progress = new WordProgress();
            progress.setUserId(userId);
            progress.setWordId(wordId);
            progress.setIsLearned(1);
            progress.setIsMastered(mastered ? 1 : 0);
            progress.setLearnedAt(now);
            progress.setReviewCount(0);
            progress.setCreatedAt(now);
            progress.setUpdatedAt(now);
            progressMapper.insert(progress);
            created = true;
        } else {
            if (progress.getIsLearned() == null || progress.getIsLearned() != 1) {
                progress.setIsLearned(1);
                progress.setLearnedAt(now);
            }
            progress.setReviewCount((progress.getReviewCount() == null ? 0 : progress.getReviewCount()) + 1);
            if (mastered) {
                progress.setIsMastered(1);
            }
            progress.setUpdatedAt(now);
            progressMapper.updateById(progress);
        }

        // 仅首次学习时刷新用户累计单词数（与 Django 一致）
        if (created) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                long learnedCount = progressMapper.selectCount(new LambdaQueryWrapper<WordProgress>()
                        .eq(WordProgress::getUserId, userId)
                        .eq(WordProgress::getIsLearned, 1));
                user.setTotalWords((int) learnedCount);
                userMapper.updateById(user);
            }
        }
        return toProgressVO(progress);
    }

    /**
     * 撤销单词学习状态（对应 Django unmark_learned）
     */
    @Transactional
    public Map<String, Object> unmarkLearned(Long userId, Long wordId) {
        WordProgress progress = progressMapper.selectOne(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getUserId, userId)
                .eq(WordProgress::getWordId, wordId));
        if (progress == null) {
            throw new BusinessException(404, "未找到学习记录");
        }
        progress.setIsLearned(0);
        progress.setIsMastered(0);
        progressMapper.updateById(progress);

        User user = userMapper.selectById(userId);
        if (user != null) {
            long learnedCount = progressMapper.selectCount(new LambdaQueryWrapper<WordProgress>()
                    .eq(WordProgress::getUserId, userId)
                    .eq(WordProgress::getIsLearned, 1));
            user.setTotalWords((int) learnedCount);
            userMapper.updateById(user);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("word_id", wordId);
        data.put("is_learned", false);
        data.put("is_mastered", false);
        return data;
    }

    /**
     * 我的学习进度（对应 Django my_progress）
     */
    public Map<String, Object> myProgress(Long userId) {
        long total = wordMapper.selectCount(null);
        long learned = progressMapper.selectCount(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getUserId, userId)
                .eq(WordProgress::getIsLearned, 1));
        long mastered = progressMapper.selectCount(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getUserId, userId)
                .eq(WordProgress::getIsMastered, 1));

        double percent = total > 0 ? Math.round(learned * 1000.0 / total) / 10.0 : 0.0;

        List<WordProgress> recent = progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getUserId, userId)
                .orderByDesc(WordProgress::getUpdatedAt)
                .last("LIMIT 20"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total_words", total);
        data.put("learned_words", learned);
        data.put("mastered_words", mastered);
        data.put("progress_percent", percent);
        data.put("recent_progress", recent.stream().map(this::toProgressVO).toList());
        return data;
    }

    /**
     * 搜索单词（对应 Django search）
     */
    public List<WordVO> search(String keyword, Long userId) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(400, "请输入搜索关键词");
        }
        List<Word> words = wordMapper.selectList(new LambdaQueryWrapper<Word>()
                .and(w -> w.like(Word::getWord, keyword).or().like(Word::getMeaning, keyword))
                .last("LIMIT 20"));
        List<WordVO> result = words.stream().map(this::toWordVO).collect(Collectors.toList());
        result.forEach(vo -> fillUserStatus(vo, userId));
        return result;
    }

    // ──── 转换与辅助 ────

    private WordVO toWordVO(Word word) {
        WordVO vo = new WordVO();
        vo.setId(word.getId());
        vo.setWord(word.getWord());
        vo.setPhonetic(word.getPhonetic());
        vo.setMeaning(word.getMeaning());
        vo.setExampleEn(word.getExampleEn());
        vo.setExampleZh(word.getExampleZh());
        vo.setAudioUrl(word.getAudioUrl());
        vo.setLevel(word.getLevel());
        vo.setCategory(word.getCategory());
        vo.setRoot(word.getRoot());
        vo.setSynonyms(word.getSynonyms());
        vo.setAntonyms(word.getAntonyms());
        vo.setWordForms(word.getWordForms());
        vo.setCreatedAt(word.getCreatedAt());
        vo.setIsLearned(false);
        vo.setIsMastered(false);
        return vo;
    }

    private void fillUserStatus(WordVO vo, Long userId) {
        if (userId == null || vo.getId() == null) {
            return;
        }
        WordProgress p = progressMapper.selectOne(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getUserId, userId)
                .eq(WordProgress::getWordId, vo.getId()));
        if (p != null) {
            vo.setIsLearned(p.getIsLearned() != null && p.getIsLearned() == 1);
            vo.setIsMastered(p.getIsMastered() != null && p.getIsMastered() == 1);
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
