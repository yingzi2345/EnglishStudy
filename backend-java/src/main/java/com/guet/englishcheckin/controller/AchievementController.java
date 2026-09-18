package com.guet.englishcheckin.controller;

import com.guet.englishcheckin.common.ApiResponse;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.service.AchievementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 成就徽章接口
 */
@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    /** 获取当前用户徽章列表（含未解锁，自动检查解锁） */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getUserAchievements(HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");
        List<Map<String, Object>> list = achievementService.getUserAchievements(currentUser.getId());
        return ApiResponse.success(list);
    }
}
