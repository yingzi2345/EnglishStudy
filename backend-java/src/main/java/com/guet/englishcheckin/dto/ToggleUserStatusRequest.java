package com.guet.englishcheckin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 禁用/启用用户请求
 */
@Data
public class ToggleUserStatusRequest {

    @NotNull(message = "user_id 不能为空")
    private Long userId;
}
