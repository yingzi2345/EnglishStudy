package com.guet.englishcheckin.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端真实 IP 提取（支持代理穿透），对应 Django 侧 middleware/ip_trace.py
 */
public final class IpUtil {

    private IpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "0.0.0.0" : remoteAddr;
    }
}
