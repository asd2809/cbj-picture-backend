package com.yupi.yupictureDDD.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupictureDDD.infrastructure.common.DeleteRequest;
import com.yupi.yupictureDDD.interfaces.dto.user.UserLoginRequest;
import com.yupi.yupictureDDD.interfaces.dto.user.UserQueryRequest;
import com.yupi.yupictureDDD.interfaces.dto.user.UserRegisterRequest;
import com.yupi.yupictureDDD.interfaces.vo.user.LoginUserVO;
import com.yupi.yupictureDDD.domain.user.entity.User;
import com.yupi.yupictureDDD.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author leneve
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-08-14 23:29:41
 */
public interface UserApplicationService{

    /**
     * 用户注册
     */
    long userRegister(UserRegisterRequest userRegisterRequest);
    /**
     * 用户登录
     *
     * @param request
     * @return 脱敏的用户信息
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest , HttpServletRequest request);
    /**
     * 获取当前的用户
     */
    User getLoginUser(HttpServletRequest request);
    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLoginOut(HttpServletRequest request);

    User getUserById(long id);

    UserVO getUserVOById(long id);

    boolean deleteUser(DeleteRequest deleteRequest);

    void updateUser(User user);

    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    List<User> listByIds(Set<Long> userIdSet);

    /**
     * 获取加密后的密码
     *
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);
    /**
     * 获得脱敏后的登录用户信息
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);
    /**
     * 获得脱敏后的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);
    /**
     * 获得脱敏后的用户列表信息
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);
    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);


    long saveUser(User user);
}
