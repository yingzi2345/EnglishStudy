package com.guet.englishcheckin.common;

import lombok.Getter;

/**
 * 业务异常：全局异常处理器统一转成 {code, message, data:null}
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
