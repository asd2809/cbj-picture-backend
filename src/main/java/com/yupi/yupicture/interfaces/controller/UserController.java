package com.yupi.yupicture.interfaces.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicture.application.service.UserApplicationService;
import com.yupi.yupicture.infrastructure.annotation.AuthCheck;
import com.yupi.yupicture.infrastructure.common.BaseResponse;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.infrastructure.common.ResultUtils;
import com.yupi.yupicture.domain.user.constant.UserConstant;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.interfaces.Assembler.UserAssembler;
import com.yupi.yupicture.interfaces.dto.user.*;
import com.yupi.yupicture.interfaces.vo.user.LoginUserVO;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.interfaces.vo.user.UserVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserApplicationService userApplicationService;
    /***
     *
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIF(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);

        long result = userApplicationService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }
    /**
     * 用户登录接口
     *
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIF(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO result = userApplicationService.userLogin(userLoginRequest, request);
        return ResultUtils.success(result);
    }
    /**
     * 获取当前用户
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        return ResultUtils.success(userApplicationService.getLoginUserVO(loginUser));
    }
    /**
     * 用户推出登录
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIF(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userApplicationService.userLoginOut(request);
        return ResultUtils.success(result);
    }
    /**
     * 创建用户
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIF(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = UserAssembler.toUserEntity(userAddRequest);

        return ResultUtils.success(userApplicationService.saveUser(user));
    }

    /**
     * 根据id获取用户(仅管理员可以)
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById( long id) {
        ThrowUtils.throwIF(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userApplicationService.getUserById(id);
        ThrowUtils.throwIF(user == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(user);
    }
    /**
     * 根据id获取包装类(主要是用户获取)
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userApplicationService.getUserVO(user));
    }
    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {

//        1.判断传入的对象是否为空
        ThrowUtils.throwIF(deleteRequest == null, ErrorCode.PARAMS_ERROR);
//        2.根据id删除用户
        boolean result = userApplicationService.deleteUser(deleteRequest);
        return ResultUtils.success(result);
    }
    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
//        1.判断传入的对象是否为空
        ThrowUtils.throwIF(userUpdateRequest == null || userUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR,"需要用户id");
        User user = UserAssembler.toUserEntity(userUpdateRequest);
        userApplicationService.updateUser(user);
        return ResultUtils.success(true);
    }

    /**
     *分页查询用户列表
     *Page<T>叫分页对象
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIF(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userApplicationService.listUserVOByPage(userQueryRequest));
    }
}
