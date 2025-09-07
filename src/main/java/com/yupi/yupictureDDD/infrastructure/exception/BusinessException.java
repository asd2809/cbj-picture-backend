package com.yupi.yupictureDDD.infrastructure.exception;


import lombok.Data;

//**
//
// 自定义异常
// */
@Data
public class BusinessException extends RuntimeException {

    //    错误码
    private final int code;

    //    构造函数
    public BusinessException(int code, final String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
