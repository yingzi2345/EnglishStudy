package com.guet.englishcheckin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信小程序登录请求
 */
@Data
public class WechatLoginRequest {

    @NotBlank(message = "code 不能为空")
    private String code;

    private String nickname;

    private String avatarUrl;
}
