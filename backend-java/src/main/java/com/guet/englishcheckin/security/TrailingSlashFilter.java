package com.guet.englishcheckin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 尾斜杠兼容过滤器：
 * 前端 api.js 所有请求路径以 "/" 结尾（如 /api/users/profile/），
 * Spring MVC 默认不做尾斜杠匹配，此处将请求 URI 规范化后再分发给 DispatcherServlet。
 */
@Component
public class TrailingSlashFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.length() > 1 && uri.endsWith("/")) {
            request.getRequestDispatcher(uri.substring(0, uri.length() - 1)).forward(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
