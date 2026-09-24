package com.guet.englishcheckin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guet.englishcheckin.common.BusinessException;
import com.guet.englishcheckin.dto.AddWordRequest;
import com.guet.englishcheckin.dto.CreateBookRequest;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.entity.UserBook;
import com.guet.englishcheckin.entity.Word;
import com.guet.englishcheckin.entity.WordBook;
import com.guet.englishcheckin.entity.WordBookItem;
import com.guet.englishcheckin.entity.WordProgress;
import com.guet.englishcheckin.mapper.UserBookMapper;
import com.guet.englishcheckin.mapper.UserMapper;
import com.guet.englishcheckin.mapper.WordBookItemMapper;
import com.guet.englishcheckin.mapper.WordBookMapper;
import com.guet.englishcheckin.mapper.WordMapper;
import com.guet.englishcheckin.mapper.WordProgressMapper;
import com.guet.englishcheckin.util.TimeUtil;
import com.guet.englishcheckin.vo.WordBookVO;
import com.guet.englishcheckin.vo.WordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 词书库 + 自定义词本服务
 * 官方词书(book_type=system)与用户自定义词本(book_type=custom)统一模型，
 * 自定义词写入 tb_word(source=custom,owner_id)，学习进度复用 tb_word_progress。
 */
@Service
@RequiredArgsConstructor
public class WordBookService {

    private final WordBookMapper bookMapper;
    private final WordBookItemMapper itemMapper;
    private final UserBookMapper userBookMapper;
    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final UserMapper userMapper;

    private static final int DEFAULT_DAILY_GOAL = 20;

    // ══════════════════════════════════════════════
    //  词书库
    // ══════════════════════════════════════════════

    /**
     * 词书库列表：所有官方词书 + 本人自定义词本，按分组排列，带学习进度
     */
    public List<WordBookVO> listBooks(Long userId) {
        // 1. 可见词书：官方 + 本人自定义
        List<WordBook> books = bookMapper.selectList(
                new LambdaQueryWrapper<WordBook>()
                        .eq(WordBook::getBookType, "system")
                        .or(w -> w.eq(WordBook::getBookType, "custom").eq(WordBook::getOwnerId, userId))
                        .orderByAsc(WordBook::getSortOrder));
        if (books.isEmpty()) {
            return new ArrayList<>();
        }
        return fillBookStats(books, userId);
    }

    /**
     * 我的词书：已加入的词书（含官方和自定义）
     */
    public List<WordBookVO> myBooks(Long userId) {
        List<UserBook> userBooks = userBookMapper.selectList(
                new LambdaQueryWrapper<UserBook>().eq(UserBook::getUserId, userId));
        if (userBooks.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> bookIds = userBooks.stream().map(UserBook::getBookId).collect(Collectors.toSet());
        List<WordBook> books = bookMapper.selectBatchIds(bookIds);
        books.sort((a, b) -> {
            int sa = a.getSortOrder() == null ? 999 : a.getSortOrder();
            int sb = b.getSortOrder() == null ? 999 : b.getSortOrder();
            return Integer.compare(sa, sb);
        });
        return fillBookStats(books, userId);
    }

    /**
     * 词书详情：统计信息 + 单词分页列表
     */
    public Map<String, Object> bookDetail(Long userId, Long bookId, long page, long pageSize) {
        WordBook book = getVisibleBook(userId, bookId);

        // 统计
        List<WordBookVO> stats = fillBookStats(Collections.singletonList(book), userId);
        WordBookVO vo = stats.isEmpty() ? toBookVO(book, Collections.emptySet(), Collections.emptySet(), Collections.emptyMap(), DEFAULT_DAILY_GOAL) : stats.get(0);

        // 单词分页
        List<WordBookItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<WordBookItem>().eq(WordBookItem::getBookId, bookId));
        Set<Long> wordIds = items.stream().map(WordBookItem::getWordId).collect(Collectors.toSet());

        Page<Word> wordPage;
        if (wordIds.isEmpty()) {
            wordPage = new Page<>(page, pageSize);
            wordPage.setRecords(new ArrayList<>());
            wordPage.setTotal(0);
        } else {
            wordPage = wordMapper.selectPage(new Page<>(page, pageSize),
                    new LambdaQueryWrapper<Word>().in(Word::getId, wordIds).orderByAsc(Word::getId));
        }
        List<WordVO> wordList = wordPage.getRecords().stream().map(this::toWordVO).collect(Collectors.toList());
        // 填充学习状态
        fillWordStatus(wordList, userId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("book", vo);
        data.put("count", wordPage.getTotal());
        data.put("results", wordList);
        return data;
    }

    /**
     * 加入词书
     */
    @Transactional
    public Map<String, Object> joinBook(Long userId, Long bookId) {
        WordBook book = getVisibleBook(userId, bookId);
        UserBook ub = userBookMapper.selectOne(
                new LambdaQueryWrapper<UserBook>()
                        .eq(UserBook::getUserId, userId)
                        .eq(UserBook::getBookId, bookId));
        boolean joined;
        if (ub == null) {
            ub = new UserBook();
            ub.setUserId(userId);
            ub.setBookId(bookId);
            ub.setIsCurrent(0);
            ub.setAddedAt(TimeUtil.nowUtc());
            userBookMapper.insert(ub);
            joined = true;
        } else {
            joined = false;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("book_id", bookId);
        data.put("is_joined", true);
        data.put("just_joined", joined);
        return data;
    }

    /**
     * 设为当前学习词书（未加入则自动加入）
     */
    @Transactional
    public Map<String, Object> selectBook(Long userId, Long bookId) {
        WordBook book = getVisibleBook(userId, bookId);
        // 先确保已加入
        UserBook ub = userBookMapper.selectOne(
                new LambdaQueryWrapper<UserBook>()
                        .eq(UserBook::getUserId, userId)
                        .eq(UserBook::getBookId, bookId));
        if (ub == null) {
            ub = new UserBook();
            ub.setUserId(userId);
            ub.setBookId(bookId);
            ub.setIsCurrent(0);
            ub.setAddedAt(TimeUtil.nowUtc());
            userBookMapper.insert(ub);
        }
        // 全部取消当前，再设这本
        List<UserBook> all = userBookMapper.selectList(
                new LambdaQueryWrapper<UserBook>().eq(UserBook::getUserId, userId));
        for (UserBook u : all) {
            u.setIsCurrent(u.getBookId().equals(bookId) ? 1 : 0);
            userBookMapper.updateById(u);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("book_id", bookId);
        data.put("is_current", true);
        return data;
    }

    // ══════════════════════════════════════════════
    //  自定义词本
    // ══════════════════════════════════════════════

    /**
     * 创建自定义词本，自动加入并设为当前学习词书
     */
    @Transactional
    public WordBookVO createCustomBook(Long userId, CreateBookRequest req) {
        if (req == null || !StringUtils.hasText(req.getName())) {
            throw new BusinessException(400, "词本名称不能为空");
        }
        String name = req.getName().trim();
        if (name.length() > 50) {
            throw new BusinessException(400, "词本名称不能超过50字");
        }
        // 重名校验（同一用户）
        Long dup = bookMapper.selectCount(
                new LambdaQueryWrapper<WordBook>()
                        .eq(WordBook::getBookType, "custom")
                        .eq(WordBook::getOwnerId, userId)
                        .eq(WordBook::getName, name));
        if (dup != null && dup > 0) {
            throw new BusinessException(400, "你已创建过同名词本");
        }

        LocalDateTime now = TimeUtil.nowUtc();
        WordBook book = new WordBook();
        book.setCode(null);
        book.setName(name);
        book.setDescription(req.getDescription() == null ? "" : req.getDescription().trim());
        book.setIcon(StringUtils.hasText(req.getIcon()) ? req.getIcon() : "📒");
        book.setBookGroup("自定义");
        book.setBookType("custom");
        book.setOwnerId(userId);
        book.setSortOrder(100);
        book.setCreatedAt(now);
        book.setUpdatedAt(now);
        bookMapper.insert(book);

        // 自动加入并设为当前
        UserBook ub = new UserBook();
        ub.setUserId(userId);
        ub.setBookId(book.getId());
        ub.setIsCurrent(1);
        ub.setAddedAt(now);
        userBookMapper.insert(ub);
        // 取消其他当前
        List<UserBook> others = userBookMapper.selectList(
                new LambdaQueryWrapper<UserBook>().eq(UserBook::getUserId, userId));
        for (UserBook u : others) {
            if (!u.getBookId().equals(book.getId())) {
                u.setIsCurrent(0);
                userBookMapper.updateById(u);
            }
        }

        List<WordBookVO> list = fillBookStats(Collections.singletonList(book), userId);
        return list.isEmpty() ? toBookVO(book, Collections.emptySet(), Collections.emptySet(), Collections.emptyMap(), DEFAULT_DAILY_GOAL) : list.get(0);
    }

    /**
     * 手动添加单词到自定义词本
     * 系统已存在的同名单词直接复用关联；不存在则创建自定义词
     */
    @Transactional
    public Map<String, Object> addCustomWord(Long userId, Long bookId, AddWordRequest req) {
        WordBook book = getOwnCustomBook(userId, bookId);
        if (req == null || !StringUtils.hasText(req.getWord())) {
            throw new BusinessException(400, "单词不能为空");
        }
        String wordText = req.getWord().trim().toLowerCase();
        if (wordText.length() > 100) {
            throw new BusinessException(400, "单词过长");
        }
        Word word = findOrCreateWord(userId, wordText,
                req.getPhonetic(), req.getMeaning(), req.getExampleEn(), req.getExampleZh());

        boolean added = linkWordToBook(bookId, word.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("word_id", word.getId());
        data.put("word", word.getWord());
        data.put("added", added);
        return data;
    }

    /**
     * 文本粘贴批量导入单词
     * 支持每行：word,释义 / word - 释义 / word：释义 / word
     */
    @Transactional
    public Map<String, Object> importWords(Long userId, Long bookId, String text) {
        WordBook book = getOwnCustomBook(userId, bookId);
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(400, "导入内容不能为空");
        }
        String[] lines = text.split("\\r?\\n");
        int success = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            // 解析分隔符：逗号(中英)、tab、" - "、"："、":"
            String wordText = null;
            String meaning = "";
            int sepIdx = findSeparator(line);
            if (sepIdx > 0) {
                wordText = line.substring(0, sepIdx).trim();
                meaning = line.substring(sepIdx + 1).trim();
                // 处理 " - " 多字符分隔
                if (meaning.startsWith("- ")) meaning = meaning.substring(2).trim();
            } else {
                wordText = line;
            }
            if (!StringUtils.hasText(wordText)) {
                skipped++;
                continue;
            }
            wordText = wordText.toLowerCase();
            if (wordText.length() > 100) {
                errors.add("第" + (i + 1) + "行：单词过长");
                skipped++;
                continue;
            }
            try {
                Word word = findOrCreateWord(userId, wordText, null, meaning, null, null);
                linkWordToBook(bookId, word.getId());
                success++;
            } catch (Exception e) {
                errors.add("第" + (i + 1) + "行：" + e.getMessage());
                skipped++;
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", success);
        data.put("skipped", skipped);
        data.put("errors", errors);
        return data;
    }

    /**
     * 删除自定义词本（仅 owner）
     */
    @Transactional
    public Map<String, Object> deleteCustomBook(Long userId, Long bookId) {
        WordBook book = getOwnCustomBook(userId, bookId);
        // 级联：item、user_book 由外键 CASCADE 自动删除
        bookMapper.deleteById(bookId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("book_id", bookId);
        data.put("deleted", true);
        return data;
    }

    /**
     * 从自定义词本移除单词（不删除单词本身）
     */
    @Transactional
    public Map<String, Object> removeWordFromBook(Long userId, Long bookId, Long wordId) {
        getOwnCustomBook(userId, bookId);
        itemMapper.delete(new LambdaQueryWrapper<WordBookItem>()
                .eq(WordBookItem::getBookId, bookId)
                .eq(WordBookItem::getWordId, wordId));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("book_id", bookId);
        data.put("word_id", wordId);
        data.put("removed", true);
        return data;
    }

    // ══════════════════════════════════════════════
    //  私有辅助
    // ══════════════════════════════════════════════

    /** 批量填充词书统计信息 */
    private List<WordBookVO> fillBookStats(List<WordBook> books, Long userId) {
        if (books.isEmpty()) return new ArrayList<>();
        Set<Long> bookIds = books.stream().map(WordBook::getId).collect(Collectors.toSet());

        // 所有 item，按 book_id 分组
        List<WordBookItem> allItems = itemMapper.selectList(
                new LambdaQueryWrapper<WordBookItem>().in(WordBookItem::getBookId, bookIds));
        Map<Long, Set<Long>> bookWordMap = new HashMap<>();
        for (WordBookItem item : allItems) {
            bookWordMap.computeIfAbsent(item.getBookId(), k -> new HashSet<>()).add(item.getWordId());
        }

        // 用户已学 wordId 集合
        Set<Long> learnedWordIds = progressMapper.selectList(
                        new LambdaQueryWrapper<WordProgress>()
                                .eq(WordProgress::getUserId, userId)
                                .eq(WordProgress::getIsLearned, 1)
                                .select(WordProgress::getWordId))
                .stream().map(WordProgress::getWordId).collect(Collectors.toSet());

        // 用户加入状态
        List<UserBook> userBooks = userBookMapper.selectList(
                new LambdaQueryWrapper<UserBook>().eq(UserBook::getUserId, userId));
        Map<Long, UserBook> userBookMap = userBooks.stream()
                .collect(Collectors.toMap(UserBook::getBookId, u -> u, (a, b) -> a));

        // 每日目标
        User user = userMapper.selectById(userId);
        int dailyGoal = (user != null && user.getDailyGoal() != null && user.getDailyGoal() > 0)
                ? user.getDailyGoal() : DEFAULT_DAILY_GOAL;

        return books.stream()
                .map(b -> toBookVO(b, bookWordMap.getOrDefault(b.getId(), Collections.emptySet()),
                        learnedWordIds, userBookMap, dailyGoal))
                .collect(Collectors.toList());
    }

    private WordBookVO toBookVO(WordBook b, Set<Long> wordIds, Set<Long> learnedIds,
                                Map<Long, UserBook> userBookMap, int dailyGoal) {
        WordBookVO vo = new WordBookVO();
        vo.setId(b.getId());
        vo.setCode(b.getCode());
        vo.setName(b.getName());
        vo.setDescription(b.getDescription());
        vo.setIcon(b.getIcon());
        vo.setBookGroup(b.getBookGroup());
        vo.setBookType(b.getBookType());
        vo.setSortOrder(b.getSortOrder());

        int total = wordIds.size();
        int learned = (int) wordIds.stream().filter(learnedIds::contains).count();
        vo.setTotalWords(total);
        vo.setLearnedWords(learned);
        vo.setUnlearnedWords(total - learned);
        vo.setEstimatedDays(total - learned > 0 ? (int) Math.ceil((double) (total - learned) / dailyGoal) : 0);

        UserBook ub = userBookMap.get(b.getId());
        vo.setIsJoined(ub != null);
        vo.setIsCurrent(ub != null && ub.getIsCurrent() != null && ub.getIsCurrent() == 1);
        return vo;
    }

    /** 获取可见词书（官方所有人可见，自定义仅 owner） */
    private WordBook getVisibleBook(Long userId, Long bookId) {
        WordBook book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "词书不存在");
        }
        if ("custom".equals(book.getBookType()) && (userId == null || !userId.equals(book.getOwnerId()))) {
            throw new BusinessException(404, "词书不存在");
        }
        return book;
    }

    /** 获取本人自定义词本 */
    private WordBook getOwnCustomBook(Long userId, Long bookId) {
        WordBook book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "词本不存在");
        }
        if (!"custom".equals(book.getBookType()) || !userId.equals(book.getOwnerId())) {
            throw new BusinessException(403, "只能操作自己的自定义词本");
        }
        return book;
    }

    /** 查找或创建单词（系统词复用，否则创建自定义词） */
    private Word findOrCreateWord(Long userId, String wordText, String phonetic, String meaning,
                                  String exampleEn, String exampleZh) {
        Word existing = wordMapper.selectOne(
                new LambdaQueryWrapper<Word>().eq(Word::getWord, wordText).last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = TimeUtil.nowUtc();
        Word word = new Word();
        word.setWord(wordText);
        word.setPhonetic(phonetic == null ? "" : phonetic);
        word.setMeaning(StringUtils.hasText(meaning) ? meaning : "（自定义词，待补充释义）");
        word.setExampleEn(exampleEn == null ? "" : exampleEn);
        word.setExampleZh(exampleZh == null ? "" : exampleZh);
        word.setAudioUrl("");
        word.setLevel(1);
        word.setCategory("自定义");
        word.setSource("custom");
        word.setOwnerId(userId);
        word.setCreatedAt(now);
        word.setUpdatedAt(now);
        wordMapper.insert(word);
        return word;
    }

    /** 关联单词到词书（已存在则跳过） */
    private boolean linkWordToBook(Long bookId, Long wordId) {
        Long exists = itemMapper.selectCount(
                new LambdaQueryWrapper<WordBookItem>()
                        .eq(WordBookItem::getBookId, bookId)
                        .eq(WordBookItem::getWordId, wordId));
        if (exists != null && exists > 0) {
            return false;
        }
        WordBookItem item = new WordBookItem();
        item.setBookId(bookId);
        item.setWordId(wordId);
        item.setCreatedAt(TimeUtil.nowUtc());
        itemMapper.insert(item);
        return true;
    }

    /** 查找行内分隔符位置，返回 -1 表示无分隔符 */
    private int findSeparator(String line) {
        // 优先中文逗号、英文逗号、tab
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ',' || c == '，' || c == '\t' || c == '：' || c == ':') {
                return i;
            }
        }
        // " - " 模式
        int idx = line.indexOf(" - ");
        if (idx > 0) return idx;
        return -1;
    }

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
        vo.setIsFavorite(false);
        return vo;
    }

    private void fillWordStatus(List<WordVO> list, Long userId) {
        if (list.isEmpty() || userId == null) return;
        Set<Long> ids = list.stream().map(WordVO::getId).collect(Collectors.toSet());
        List<WordProgress> progresses = progressMapper.selectList(
                new LambdaQueryWrapper<WordProgress>()
                        .eq(WordProgress::getUserId, userId)
                        .in(WordProgress::getWordId, ids));
        Map<Long, WordProgress> map = progresses.stream()
                .collect(Collectors.toMap(WordProgress::getWordId, p -> p, (a, b) -> a));
        for (WordVO vo : list) {
            WordProgress p = map.get(vo.getId());
            if (p != null) {
                vo.setIsLearned(p.getIsLearned() != null && p.getIsLearned() == 1);
                vo.setIsMastered(p.getIsMastered() != null && p.getIsMastered() == 1);
                vo.setIsFavorite(p.getIsFavorite() != null && p.getIsFavorite() == 1);
            }
        }
    }
}
