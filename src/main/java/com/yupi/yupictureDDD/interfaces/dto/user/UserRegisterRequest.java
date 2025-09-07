package com.yupi.yupictureDDD.interfaces.dto.user;


import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 */
@Data
public class UserRegisterRequest implements Serializable {
    //不是很重要
    private static final long serialVersionUID = 4504888962465602797L;
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 密码
     */
    private String userPassword;
    /**
     * 确认密码
     */
    private String checkPassword;


}
