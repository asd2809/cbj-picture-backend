package com.yupi.cbjpicturebackend.common;

import com.yupi.cbjpicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局相应封装类
 *
 * @param <T>
 */
@Data
//implements表示支持序列化
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;


    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    //    返回给前端错误码
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
