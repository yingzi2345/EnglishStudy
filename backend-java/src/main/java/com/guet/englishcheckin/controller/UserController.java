package com.guet.englishcheckin.controller;

import com.guet.englishcheckin.common.ApiResponse;
import com.guet.englishcheckin.dto.UpdateProfileRequest;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.security.JwtAuthInterceptor;
import com.guet.englishcheckin.service.UserService;
import com.guet.englishcheckin.vo.LoginLogVO;
import com.guet.englishcheckin.vo.UserProfileVO;
import com.guet.englishcheckin.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户模块接口：对应 Django UserViewSet
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** GET /api/users/ 用户列表（登录用户可见，管理端使用） */
    @GetMapping
    public ApiResponse<List<UserVO>> list() {
        return ApiResponse.success(userService.listUsers());
    }

    /** POST /api/users/ 创建用户（DRF create 兼容） */
    @PostMapping
    public ApiResponse<UserVO> create(@RequestBody User user) {
        return ApiResponse.success(userService.createUser(user));
    }

    /** GET /api/users/profile/ 获取当前用户个人信息 */
    @GetMapping("/profile")
    public ApiResponse<UserProfileVO> profile(HttpServletRequest request) {
        return ApiResponse.success(userService.getProfile(currentUserId(request)));
    }

    /** PUT /api/users/update_profile/ 更新个人信息 */
    @PutMapping("/update_profile")
    public ApiResponse<UserProfileVO> updateProfile(@RequestBody UpdateProfileRequest req,
                                                    HttpServletRequest request) {
        return ApiResponse.success(userService.updateProfile(currentUserId(request), req));
    }

    /** GET /api/users/login_logs/ 当前用户登录日志 */
    @GetMapping("/login_logs")
    public ApiResponse<List<LoginLogVO>> loginLogs(HttpServletRequest request) {
        return ApiResponse.success(userService.getLoginLogs(currentUserId(request)));
    }

    /** GET /api/users/stats/ 学习统计 */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(HttpServletRequest request) {
        return ApiResponse.success(userService.getStats(currentUserId(request)));
    }

    private Long currentUserId(HttpServletRequest request) {
        User user = (User) request.getAttribute(JwtAuthInterceptor.CURRENT_USER_ATTR);
        return user == null ? null : user.getId();
    }
}
