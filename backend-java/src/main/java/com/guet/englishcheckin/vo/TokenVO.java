package com.guet.englishcheckin.vo;

import lombok.Data;

/**
 * JWT Token 对（前端取 data.token.access）
 */
@Data
public class TokenVO {

    private String access;
    private String refresh;

    public TokenVO(String access, String refresh) {
        this.access = access;
        this.refresh = refresh;
    }
}
