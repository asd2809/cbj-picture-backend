package com.yupi.yupictureDDD.interfaces.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新用户请求
 */
@Data
public class UserUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     *因为修改用户的只能是管理员，所以，为了在修改的时候
     *不会因为忘记把用户权限填加上，在这个类中用户权限默认为admin
     */

    private final String userRole = "admin";

    private static final long serialversionUID = 1L;
}
