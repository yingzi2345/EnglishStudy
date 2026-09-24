package com.guet.englishcheckin.controller;

import com.guet.englishcheckin.common.ApiResponse;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.security.JwtAuthInterceptor;
import com.guet.englishcheckin.service.StudyService;
import com.guet.englishcheckin.vo.QuizQuestionVO;
import com.guet.englishcheckin.vo.QuizResultVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 学习系统接口：卡片学习、艾宾浩斯复习、测验、错词本
 */
@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    /** GET /api/study/today/ 今日学习任务（待复习+新词） */
    @GetMapping("/today")
    public ApiResponse<Map<String, Object>> todayTasks(
            @RequestParam(defaultValue = "10") int review_count,
            @RequestParam(defaultValue = "10") int new_count,
            HttpServletRequest request) {
        return ApiResponse.success(studyService.getTodayTasks(currentUserId(request), review_count, new_count));
    }

    /** POST /api/study/record/ 提交学习结果 {word_id, grade: easy/vague/hard}（兼容旧 known 字段） */
    @PostMapping("/record")
    public ApiResponse<Map<String, Object>> record(@RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
        Object wordIdObj = body == null ? null : body.get("word_id");
        if (wordIdObj == null) {
            return ApiResponse.error(400, "参数错误：缺少 word_id");
        }
        Long wordId = Long.valueOf(wordIdObj.toString());
        String grade = body.get("grade") == null ? null : body.get("grade").toString();
        Boolean known = body.get("known") == null ? null : Boolean.parseBoolean(body.get("known").toString());
        return ApiResponse.success(studyService.submitStudyResult(currentUserId(request), wordId, grade, known));
    }

    /** GET /api/study/quiz/ 获取测验题目 */
    @GetMapping("/quiz")
    public ApiResponse<List<QuizQuestionVO>> quiz(@RequestParam(defaultValue = "10") int count,
                                                  HttpServletRequest request) {
        return ApiResponse.success(studyService.getQuizQuestions(currentUserId(request), count));
    }

    /** POST /api/study/quiz/submit/ 提交测验答案 */
    @PostMapping("/quiz/submit")
    public ApiResponse<QuizResultVO> quizSubmit(@RequestBody Map<String, Object> body,
                                                HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        if (answers == null || answers.isEmpty()) {
            return ApiResponse.error(400, "答案不能为空");
        }
        return ApiResponse.success(studyService.submitQuiz(currentUserId(request), answers));
    }

    /** GET /api/study/wrong-words/ 错词本列表 */
    @GetMapping("/wrong-words")
    public ApiResponse<Map<String, Object>> wrongWords(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(name = "page_size", defaultValue = "20") long pageSize,
            HttpServletRequest request) {
        return ApiResponse.success(studyService.getWrongWords(currentUserId(request), page, pageSize));
    }

    /** POST /api/study/wrong-words/remove/ 从错词本移除 */
    @PostMapping("/wrong-words/remove")
    public ApiResponse<Map<String, Object>> removeWrongWord(@RequestBody Map<String, Object> body,
                                                            HttpServletRequest request) {
        Object wordIdObj = body == null ? null : body.get("word_id");
        if (wordIdObj == null) {
            return ApiResponse.error(400, "参数错误：缺少 word_id");
        }
        Long wordId = Long.valueOf(wordIdObj.toString());
        return ApiResponse.success(studyService.removeWrongWord(currentUserId(request), wordId));
    }

    /** POST /api/study/favorite/ 收藏 / 取消收藏单词 {word_id} */
    @PostMapping("/favorite")
    public ApiResponse<Map<String, Object>> toggleFavorite(@RequestBody Map<String, Object> body,
                                                           HttpServletRequest request) {
        Object wordIdObj = body == null ? null : body.get("word_id");
        if (wordIdObj == null) {
            return ApiResponse.error(400, "参数错误：缺少 word_id");
        }
        Long wordId = Long.valueOf(wordIdObj.toString());
        return ApiResponse.success(studyService.toggleFavorite(currentUserId(request), wordId));
    }

    /** GET /api/study/favorites/ 收藏单词列表 */
    @GetMapping("/favorites")
    public ApiResponse<Map<String, Object>> favorites(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(name = "page_size", defaultValue = "20") long pageSize,
            HttpServletRequest request) {
        return ApiResponse.success(studyService.getFavoriteWords(currentUserId(request), page, pageSize));
    }

    /** GET /api/study/stats/ 今日学习统计（待复习数/错词数/已掌握数） */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(HttpServletRequest request) {
        return ApiResponse.success(studyService.getTodayStats(currentUserId(request)));
    }

    private Long currentUserId(HttpServletRequest request) {
        User user = (User) request.getAttribute(JwtAuthInterceptor.CURRENT_USER_ATTR);
        return user == null ? null : user.getId();
    }
}
