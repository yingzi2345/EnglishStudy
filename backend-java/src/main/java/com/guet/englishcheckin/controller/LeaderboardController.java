package com.guet.englishcheckin.controller;

import com.guet.englishcheckin.common.ApiResponse;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.security.JwtAuthInterceptor;
import com.guet.englishcheckin.service.LeaderboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 排行榜接口：对应 Django LeaderboardViewSet
 */
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/daily")
    public ApiResponse<Map<String, Object>> daily(HttpServletRequest request) {
        return ApiResponse.success(leaderboardService.rank("daily", currentUserId(request)));
    }

    @GetMapping("/weekly")
    public ApiResponse<Map<String, Object>> weekly(HttpServletRequest request) {
        return ApiResponse.success(leaderboardService.rank("weekly", currentUserId(request)));
    }

    @GetMapping("/monthly")
    public ApiResponse<Map<String, Object>> monthly(HttpServletRequest request) {
        return ApiResponse.success(leaderboardService.rank("monthly", currentUserId(request)));
    }

    @GetMapping("/alltime")
    public ApiResponse<Map<String, Object>> alltime(HttpServletRequest request) {
        return ApiResponse.success(leaderboardService.rank("alltime", currentUserId(request)));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(leaderboardService.stats());
    }

    private Long currentUserId(HttpServletRequest request) {
        User user = (User) request.getAttribute(JwtAuthInterceptor.CURRENT_USER_ATTR);
        return user == null ? null : user.getId();
    }
}
