package com.guet.englishcheckin.controller;

import com.guet.englishcheckin.common.ApiResponse;
import com.guet.englishcheckin.dto.AdminLoginRequest;
import com.guet.englishcheckin.dto.WechatLoginRequest;
import com.guet.englishcheckin.security.IpUtil;
import com.guet.englishcheckin.service.AdminService;
import com.guet.englishcheckin.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口（公开，无需登录）：对应 Django apps/users/urls.py
 * - POST /api/auth/wechat-login/  微信小程序登录
 * - POST /api/auth/admin-login/   管理员登录
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AdminService adminService;

    @PostMapping("/wechat-login")
    public ApiResponse<Map<String, Object>> wechatLogin(@Valid @RequestBody WechatLoginRequest req,
                                                        HttpServletRequest request) {
        String ip = IpUtil.getClientIp(request);
        String deviceInfo = request.getHeader("User-Agent");
        return ApiResponse.success(userService.wechatLogin(req, ip, deviceInfo));
    }

    @PostMapping("/admin-login")
    public ApiResponse<Map<String, Object>> adminLogin(@Valid @RequestBody AdminLoginRequest req,
                                                       HttpServletRequest request) {
        String ip = IpUtil.getClientIp(request);
        return ApiResponse.success(adminService.login(req.getUsername(), req.getPassword(), ip));
    }
}
