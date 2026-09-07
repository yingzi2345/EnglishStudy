package com.guet.englishcheckin.controller;

import com.guet.englishcheckin.common.ApiResponse;
import com.guet.englishcheckin.dto.MarkWordRequest;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.entity.Word;
import com.guet.englishcheckin.security.JwtAuthInterceptor;
import com.guet.englishcheckin.service.WordService;
import com.guet.englishcheckin.vo.WordProgressVO;
import com.guet.englishcheckin.vo.WordVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 单词模块接口：对应 Django WordViewSet
 */
@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    /** GET /api/words/ 单词分页列表（category / search / page / page_size） */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String category,
                                                 @RequestParam(required = false) String search,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(name = "page_size", defaultValue = "20") long pageSize) {
        return ApiResponse.success(wordService.list(category, search, page, pageSize));
    }

    /** GET /api/words/{id}/ 单词详情 */
    @GetMapping("/{id}")
    public ApiResponse<WordVO> retrieve(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(wordService.retrieve(id, currentUserId(request)));
    }

    /** POST /api/words/ 创建单词 */
    @PostMapping
    public ApiResponse<WordVO> create(@RequestBody Word word) {
        return ApiResponse.success(wordService.create(word));
    }

    /** PUT /api/words/{id}/ 更新单词 */
    @PutMapping("/{id}")
    public ApiResponse<WordVO> update(@PathVariable Long id, @RequestBody Word word) {
        return ApiResponse.success(wordService.update(id, word));
    }

    /** DELETE /api/words/{id}/ 删除单词 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        wordService.delete(id);
        return ApiResponse.success(null);
    }

    /** GET /api/words/categories/ 所有分类 */
    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.success(wordService.categories());
    }

    /** GET /api/words/random_word/ 随机单词 */
    @GetMapping("/random_word")
    public ApiResponse<WordVO> randomWord(@RequestParam(required = false) Integer level,
                                          @RequestParam(required = false) String category,
                                          HttpServletRequest request) {
        return ApiResponse.success(wordService.randomWord(level, category, currentUserId(request)));
    }

    /** GET /api/words/daily_words/ 每日推荐单词 */
    @GetMapping("/daily_words")
    public ApiResponse<List<WordVO>> dailyWords(@RequestParam(defaultValue = "10") int count,
                                                HttpServletRequest request) {
        return ApiResponse.success(wordService.dailyWords(count, currentUserId(request)));
    }

    /** POST /api/words/mark_learned/ 标记已学 */
    @PostMapping("/mark_learned")
    public ApiResponse<WordProgressVO> markLearned(@Valid @RequestBody MarkWordRequest req,
                                                   HttpServletRequest request) {
        return ApiResponse.success(wordService.markLearned(currentUserId(request), req.getWordId(), req.getIsMastered()));
    }

    /** POST /api/words/unmark_learned/ 撤销学习 */
    @PostMapping("/unmark_learned")
    public ApiResponse<Map<String, Object>> unmarkLearned(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        Object wordIdObj = body == null ? null : body.get("word_id");
        if (wordIdObj == null) {
            return ApiResponse.error(400, "参数错误");
        }
        Long wordId = Long.valueOf(wordIdObj.toString());
        return ApiResponse.success(wordService.unmarkLearned(currentUserId(request), wordId));
    }

    /** GET /api/words/my_progress/ 我的学习进度 */
    @GetMapping("/my_progress")
    public ApiResponse<Map<String, Object>> myProgress(HttpServletRequest request) {
        return ApiResponse.success(wordService.myProgress(currentUserId(request)));
    }

    /** GET /api/words/search/ 搜索单词 */
    @GetMapping("/search")
    public ApiResponse<List<WordVO>> search(@RequestParam String q, HttpServletRequest request) {
        return ApiResponse.success(wordService.search(q, currentUserId(request)));
    }

    private Long currentUserId(HttpServletRequest request) {
        User user = (User) request.getAttribute(JwtAuthInterceptor.CURRENT_USER_ATTR);
        return user == null ? null : user.getId();
    }
}
