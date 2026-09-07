package com.guet.englishcheckin.security;

import com.guet.englishcheckin.common.ApiResponse;
import com.guet.englishcheckin.entity.User;
import com.guet.englishcheckin.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * JWT 认证拦截器（对应 Django 侧 utils/authentication.py CustomJWTAuthentication）：
 * - 从 Authorization: Bearer <token> 解析 user_id 并在 tb_user 查询用户
 * - 用户不存在或已禁用（status=0）时拒绝
 * - 认证失败统一返回 HTTP 401 + {code:401}（前端据此清 token 跳登录页）
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ATTR = "currentUser";

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 CORS 预检请求
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isEmpty()) {
                try {
                    Claims claims = jwtUtil.parseToken(token);
                    Long userId = claims.get("user_id", Long.class);
                    if (userId == null) {
                        return reject(response, "Token 中缺少 user_id");
                    }
                    User user = userMapper.selectById(userId);
                    if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                        return reject(response, "用户不存在或已被禁用");
                    }
                    request.setAttribute(CURRENT_USER_ATTR, user);
                    return true;
                } catch (JwtException | IllegalArgumentException e) {
                    return reject(response, "登录已过期，请重新登录");
                }
            }
        }
        return reject(response, "未认证，请先登录");
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(401, message)));
        return false;
    }
}
