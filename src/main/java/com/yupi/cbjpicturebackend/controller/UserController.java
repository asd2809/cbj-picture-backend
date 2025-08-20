package com.yupi.cbjpicturebackend.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.cbjpicturebackend.annotation.AuthCheck;
import com.yupi.cbjpicturebackend.common.BaseResponse;
import com.yupi.cbjpicturebackend.common.DeleteRequest;
import com.yupi.cbjpicturebackend.common.ResultUtils;
import com.yupi.cbjpicturebackend.constant.UserConstant;
import com.yupi.cbjpicturebackend.exception.ErrorCode;
import com.yupi.cbjpicturebackend.exception.ThrowUtils;
import com.yupi.cbjpicturebackend.model.dto.user.*;
import com.yupi.cbjpicturebackend.model.entity.LoginUserVO;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.model.vo.UserVO;
import com.yupi.cbjpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /***
     *
     * 用户注册
     */

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIF(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
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

        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();


        LoginUserVO result = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(result);
    }
    /**
     * 获取当前用户
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> userGetLogin(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }
    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIF(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLoginOut(request);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
//    添加权限
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIF(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
//        把web请求传入的对象转换给user
        BeanUtils.copyProperties(userAddRequest, user);
//        默认密码
        final String DEFAULT_PASSWORD = "123456789";
//        加密处理
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
//        把数据保存到数据库中
        boolean result = userService.save(user);
        ThrowUtils.throwIF(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据id获取用户(仅管理员可以)
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById( long id) {
        ThrowUtils.throwIF(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
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
        return ResultUtils.success(userService.getUserVO(user));
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
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }
    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
//        1.判断传入的对象是否为空
        ThrowUtils.throwIF(userUpdateRequest == null || userUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIF(!result, ErrorCode.OPERATION_ERROR);
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
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
//        进行分页查询
//        第二个参数的查询的条件,之前已经在userService写好了
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
//        转化为我们需要的脱敏类的列表
        Page<UserVO> userVOPage =new Page<>(current,pageSize,userPage.getTotal());
//        写好的列表用户转化为脱敏的列表用户
//        getRecords()方法是Page内置的一个转换为list的方法
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
//        setRecords这个方法是把List<T>转换为Page<T>
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
}
