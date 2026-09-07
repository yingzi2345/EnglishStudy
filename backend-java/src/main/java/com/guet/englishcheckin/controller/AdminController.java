package com.guet.englishcheckin.controller;

import com.guet.englishcheckin.common.ApiResponse;
import com.guet.englishcheckin.dto.AdminLoginRequest;
import com.guet.englishcheckin.dto.ToggleUserStatusRequest;
import com.guet.englishcheckin.security.IpUtil;
import com.guet.englishcheckin.service.AdminService;
import com.guet.englishcheckin.vo.LoginLogVO;
import com.guet.englishcheckin.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理员模块接口：对应 Django AdminViewSet
 */
@RestController
@RequestMapping("/api/admin-users")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** POST /api/admin-users/login/ 管理员登录（公开） */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest req,
                                                  HttpServletRequest request) {
        return ApiResponse.success(adminService.login(req.getUsername(), req.getPassword(),
                IpUtil.getClientIp(request)));
    }

    /** GET /api/admin-users/dashboard/ 管理后台首页数据 */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.success(adminService.dashboard());
    }

    /** GET /api/admin-users/user_list/ 用户列表 */
    @GetMapping("/user_list")
    public ApiResponse<List<UserVO>> userList() {
        return ApiResponse.success(adminService.userList());
    }

    /** GET /api/admin-users/login_log_list/ 全部登录日志 */
    @GetMapping("/login_log_list")
    public ApiResponse<List<LoginLogVO>> loginLogList() {
        return ApiResponse.success(adminService.loginLogList());
    }

    /** POST /api/admin-users/toggle_user_status/ 禁用/启用用户 */
    @PostMapping("/toggle_user_status")
    public ApiResponse<Void> toggleUserStatus(@Valid @RequestBody ToggleUserStatusRequest req) {
        return ApiResponse.success(adminService.toggleUserStatus(req.getUserId()), null);
    }
}
