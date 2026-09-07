package com.guet.englishcheckin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（application.yml 的 jwt.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥（HS256，长度 >= 32 字节） */
    private String secret;

    /** access token 有效期（秒），默认 2 小时 */
    private long accessTokenExpireSeconds = 7200;

    /** refresh token 有效期（秒），默认 7 天 */
    private long refreshTokenExpireSeconds = 604800;
}
