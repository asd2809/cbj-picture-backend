package com.yupi.yupictureDDD.domain.user.constant;

//目前是用于session与权限控制
public interface UserConstant {
    /**
     * 用户登录太键
     */
    String USER_LOGIN_STATE = "user_login_state";

    //region 权限

    /**
     * 默认角色
     */

    String DEFAULT_ROLE = "user";
    /***
     *
     *管理员角色
     */
    String ADMIN_ROLE = "admin";
    // endregion
}
