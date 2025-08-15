package com.yupi.cbjpicturebackend.exception;

/**
 * 异常处理工具类
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     *
     * @param condition        条件
     * @param runtimeException 异常
     */
    public static void throwIF(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void throwIF(boolean condition, ErrorCode errorCode) {
//        第二个参数是自己定义的错误
        throwIF(condition, new BusinessException(errorCode));
    }

    /**
     * @param condition 条件
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public static void throwIFf(boolean condition, ErrorCode errorCode, String message) {
        throwIF(condition, new BusinessException(errorCode, message));
    }

}
