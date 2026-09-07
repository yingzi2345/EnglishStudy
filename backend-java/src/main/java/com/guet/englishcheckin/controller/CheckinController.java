package com.guet.englishcheckin.controller;

import com.guet.englishcheckin.common.ApiResponse;
import com.guet.englishcheckin.dto.CheckinRequest;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.security.JwtAuthInterceptor;
import com.guet.englishcheckin.service.CheckinService;
import com.guet.englishcheckin.vo.CheckinVO;
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
 * 打卡模块接口：对应 Django CheckinViewSet
 */
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    /** GET /api/checkin/ 当前用户打卡列表（DRF list 兼容） */
    @GetMapping
    public ApiResponse<List<CheckinVO>> list(HttpServletRequest request) {
        return ApiResponse.success(checkinService.listByUser(currentUserId(request)));
    }

    /** POST /api/checkin/do_checkin/ 执行每日打卡 */
    @PostMapping("/do_checkin")
    public ApiResponse<Map<String, Object>> doCheckin(@RequestBody CheckinRequest req,
                                                      HttpServletRequest request) {
        return ApiResponse.success(checkinService.doCheckin(currentUserId(request), req));
    }

    /** GET /api/checkin/today_status/ 今日打卡状态 */
    @GetMapping("/today_status")
    public ApiResponse<Map<String, Object>> todayStatus(HttpServletRequest request) {
        return ApiResponse.success(checkinService.todayStatus(currentUserId(request)));
    }

    /** GET /api/checkin/calendar/ 打卡日历 */
    @GetMapping("/calendar")
    public ApiResponse<Map<String, Object>> calendar(@RequestParam(required = false) Integer year,
                                                     @RequestParam(required = false) Integer month,
                                                     HttpServletRequest request) {
        return ApiResponse.success(checkinService.calendar(currentUserId(request), year, month));
    }

    /** GET /api/checkin/records/ 打卡记录列表 */
    @GetMapping("/records")
    public ApiResponse<List<CheckinVO>> records(HttpServletRequest request) {
        return ApiResponse.success(checkinService.records(currentUserId(request)));
    }

    /** GET /api/checkin/streak/ 连续打卡信息 */
    @GetMapping("/streak")
    public ApiResponse<Map<String, Object>> streak(HttpServletRequest request) {
        return ApiResponse.success(checkinService.streak(currentUserId(request)));
    }

    private Long currentUserId(HttpServletRequest request) {
        User user = (User) request.getAttribute(JwtAuthInterceptor.CURRENT_USER_ATTR);
        return user == null ? null : user.getId();
    }
}
