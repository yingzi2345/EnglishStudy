package com.guet.englishcheckin.controller;

import com.guet.englishcheckin.common.ApiResponse;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.security.JwtAuthInterceptor;
import com.guet.englishcheckin.service.StatsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 学习数据统计接口：打卡热力图、学习趋势等可视化数据
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** GET /api/stats/analytics/ 最近 30 天学习数据（热力图 + 趋势） */
    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> analytics(HttpServletRequest request) {
        return ApiResponse.success(statsService.analytics(currentUserId(request)));
    }

    private Long currentUserId(HttpServletRequest request) {
        User user = (User) request.getAttribute(JwtAuthInterceptor.CURRENT_USER_ATTR);
        return user == null ? null : user.getId();
    }
}
