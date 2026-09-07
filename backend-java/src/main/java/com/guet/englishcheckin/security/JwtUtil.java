package com.guet.englishcheckin.security;

import com.guet.englishcheckin.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 工具：签发与解析（HS256）
 * access 2h / refresh 7d，与 Django SimpleJWT 配置对齐；
 * payload 携带 user_id 与 openid，供认证拦截器使用。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessExpireSeconds;
    private final long refreshExpireSeconds;

    public JwtUtil(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessExpireSeconds = properties.getAccessTokenExpireSeconds();
        this.refreshExpireSeconds = properties.getRefreshTokenExpireSeconds();
    }

    /**
     * 生成 access token（默认有效期 2 小时）
     */
    public String generateAccessToken(Long userId, String openid) {
        return generateToken(userId, openid, accessExpireSeconds);
    }

    /**
     * 生成 refresh token（默认有效期 7 天）
     */
    public String generateRefreshToken(Long userId, String openid) {
        return generateToken(userId, openid, refreshExpireSeconds);
    }

    private String generateToken(Long userId, String openid, long expireSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("user_id", userId)
                .claim("openid", openid == null ? "" : openid)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析并校验 token，返回 Claims；无效/过期时抛出 JwtException
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }
}
