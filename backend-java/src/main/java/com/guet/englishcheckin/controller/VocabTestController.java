package com.guet.englishcheckin.controller;

import com.guet.englishcheckin.common.ApiResponse;
import com.guet.englishcheckin.service.VocabTestService;
import com.guet.englishcheckin.vo.QuizQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 词汇量测试接口
 */
@RestController
@RequestMapping("/api/vocab-test")
@RequiredArgsConstructor
public class VocabTestController {

    private final VocabTestService vocabTestService;

    /** 获取测试题目（默认30题） */
    @GetMapping("/questions")
    public ApiResponse<List<QuizQuestionVO>> getQuestions(
            @RequestParam(name = "count", defaultValue = "30") int count) {
        return ApiResponse.success(vocabTestService.getQuestions(count));
    }

    /** 提交答案，返回词汇量估算结果 */
    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        return ApiResponse.success(vocabTestService.submitAnswers(answers));
    }
}
