package com.yupi.yupictureDDD.interfaces.vo.user;


import lombok.Data;

import java.io.Serializable;

@Data
public class UserVO implements Serializable {
    /***
     * id
     */
    private String id;
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;
    /***
     * 用户简介
     *
     */
    private String userProfile;
    /**
     * 用户角色:admin/user
     */
    private String userRole;
    /**
     * 创建时间
     */
    private String createTime;
    private static final long serialVersionUID = 1L;
}
