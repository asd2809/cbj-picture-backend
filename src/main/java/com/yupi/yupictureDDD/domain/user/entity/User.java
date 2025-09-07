package com.yupi.yupictureDDD.domain.user.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import com.yupi.yupictureDDD.domain.user.constant.UserConstant;
import com.yupi.yupictureDDD.infrastructure.exception.BusinessException;
import com.yupi.yupictureDDD.infrastructure.exception.ErrorCode;
import lombok.Data;

/**
 * 用户
 *
 * @TableName user
 */
@TableName(value = "user")
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */

    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    @TableField(exist = false)//对应数据库列名子
    private Integer isDelete;

    private static final long serialVersionUID = 1L;

    /**
     * 注册校验
     * @param userAccount
     * @param userPassword
     * @param userCheckPassword
     */
    public static void validUserRegister(String userAccount, String userPassword, String userCheckPassword){
        if (StrUtil.hasBlank(userAccount, userPassword, userCheckPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 6 || userCheckPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(userCheckPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不对");
        }
    }

    /**
     * 登录校验
     * @param userAccount
     * @param userPassword
     */
    public static void validUserLogin(String userAccount, String userPassword){
        //1.校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
    }

    /**
     * 判断用户是否为管理员
     * @return
     */
    public boolean isAdmin(){
        return UserConstant.ADMIN_ROLE.equals(this.getUserRole());
    }
}