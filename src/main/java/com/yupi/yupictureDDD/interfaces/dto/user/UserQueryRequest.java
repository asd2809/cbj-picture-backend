package com.yupi.yupictureDDD.interfaces.dto.user;


import com.yupi.yupictureDDD.infrastructure.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户查询请求
 * 加的字段，是你觉得可以根据什么查询用户
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */

    private String userAccount;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色；admin/user/ban
     */

    private String userRole;
    private static final long serialVersionUID = 1L;

}
