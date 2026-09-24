package com.guet.englishcheckin.controller;

import com.guet.englishcheckin.common.ApiResponse;
import com.guet.englishcheckin.dto.AddWordRequest;
import com.guet.englishcheckin.dto.CreateBookRequest;
import com.guet.englishcheckin.dto.ImportWordsRequest;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.security.JwtAuthInterceptor;
import com.guet.englishcheckin.service.WordBookService;
import com.guet.englishcheckin.vo.WordBookVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 词书库 + 自定义词本接口
 */
@RestController
@RequestMapping("/api/word-books")
@RequiredArgsConstructor
public class WordBookController {

    private final WordBookService wordBookService;

    /** GET /api/word-books/ 词书库列表（官方 + 本人自定义） */
    @GetMapping
    public ApiResponse<List<WordBookVO>> list(HttpServletRequest request) {
        return ApiResponse.success(wordBookService.listBooks(currentUserId(request)));
    }

    /** GET /api/word-books/mine/ 我的词书（已加入） */
    @GetMapping("/mine")
    public ApiResponse<List<WordBookVO>> mine(HttpServletRequest request) {
        return ApiResponse.success(wordBookService.myBooks(currentUserId(request)));
    }

    /** GET /api/word-books/{id}/ 词书详情 + 单词分页 */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(name = "page_size", defaultValue = "20") long pageSize,
                                                   HttpServletRequest request) {
        return ApiResponse.success(wordBookService.bookDetail(currentUserId(request), id, page, pageSize));
    }

    /** POST /api/word-books/{id}/join/ 加入词书 */
    @PostMapping("/{id}/join")
    public ApiResponse<Map<String, Object>> join(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(wordBookService.joinBook(currentUserId(request), id));
    }

    /** POST /api/word-books/{id}/select/ 设为当前学习词书 */
    @PostMapping("/{id}/select")
    public ApiResponse<Map<String, Object>> select(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(wordBookService.selectBook(currentUserId(request), id));
    }

    /** POST /api/word-books/custom/ 创建自定义词本 */
    @PostMapping("/custom")
    public ApiResponse<WordBookVO> createCustom(@RequestBody CreateBookRequest req, HttpServletRequest request) {
        return ApiResponse.success(wordBookService.createCustomBook(currentUserId(request), req));
    }

    /** POST /api/word-books/{id}/words/ 手动添加单词到自定义词本 */
    @PostMapping("/{id}/words")
    public ApiResponse<Map<String, Object>> addWord(@PathVariable Long id,
                                                    @RequestBody AddWordRequest req,
                                                    HttpServletRequest request) {
        return ApiResponse.success(wordBookService.addCustomWord(currentUserId(request), id, req));
    }

    /** POST /api/word-books/{id}/import/ 文本粘贴批量导入 */
    @PostMapping("/{id}/import")
    public ApiResponse<Map<String, Object>> importWords(@PathVariable Long id,
                                                        @RequestBody ImportWordsRequest req,
                                                        HttpServletRequest request) {
        return ApiResponse.success(wordBookService.importWords(currentUserId(request), id,
                req == null ? null : req.getText()));
    }

    /** DELETE /api/word-books/{id}/ 删除自定义词本 */
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(wordBookService.deleteCustomBook(currentUserId(request), id));
    }

    /** DELETE /api/word-books/{id}/words/{wordId}/ 从自定义词本移除单词 */
    @DeleteMapping("/{id}/words/{wordId}")
    public ApiResponse<Map<String, Object>> removeWord(@PathVariable Long id,
                                                       @PathVariable Long wordId,
                                                       HttpServletRequest request) {
        return ApiResponse.success(wordBookService.removeWordFromBook(currentUserId(request), id, wordId));
    }

    private Long currentUserId(HttpServletRequest request) {
        User user = (User) request.getAttribute(JwtAuthInterceptor.CURRENT_USER_ATTR);
        return user == null ? null : user.getId();
    }
}
