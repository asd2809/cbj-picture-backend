package com.yupi.yupictureDDD.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupictureDDD.application.service.UserApplicationService;
import com.yupi.yupictureDDD.domain.user.service.UserDomainService;
import com.yupi.yupictureDDD.infrastructure.common.DeleteRequest;
import com.yupi.yupictureDDD.infrastructure.exception.BusinessException;
import com.yupi.yupictureDDD.infrastructure.exception.ErrorCode;
import com.yupi.yupictureDDD.infrastructure.exception.ThrowUtils;
import com.yupi.yupictureDDD.interfaces.dto.user.UserLoginRequest;
import com.yupi.yupictureDDD.interfaces.dto.user.UserQueryRequest;
import com.yupi.yupictureDDD.interfaces.dto.user.UserRegisterRequest;
import com.yupi.yupictureDDD.interfaces.vo.user.LoginUserVO;
import com.yupi.yupictureDDD.domain.user.entity.User;
import com.yupi.yupictureDDD.interfaces.vo.user.UserVO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author leneve
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-08-14 23:29:41
 */
@Service
@Slf4j
public class UserApplicationServiceImpl implements UserApplicationService {

    @Resource
    private UserDomainService userDomainService;
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        //1.校验参数
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        User.validUserRegister(userAccount,userPassword,checkPassword);
        return userDomainService.userRegister(userAccount,userPassword,checkPassword);
    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        User.validUserLogin(userAccount,userPassword);
        return userDomainService.userLogin(userAccount,userPassword,request);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        return userDomainService.getLoginUser(request);
    }

    /**
     * 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        return userDomainService.getLoginUserVO(user);

    }

    @Override
    public UserVO getUserVO(User user) {
        return userDomainService.getUserVO(user);

    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        return userDomainService.getUserVOList(userList);

    }

    @Override
    public boolean userLoginOut(HttpServletRequest request) {
        return userDomainService.userLoginOut(request);
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        return userDomainService.getQueryWrapper(userQueryRequest);
    }

    @Override
    public long saveUser(User user) {
        //        默认密码
        final String DEFAULT_PASSWORD = "123456789";
//        加密处理
        String encryptPassword = userDomainService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
//        把数据保存到数据库中
        boolean result = userDomainService.saveUser(user);
        ThrowUtils.throwIF(!result,ErrorCode.PARAMS_ERROR,"数据库操作失败");
        return user.getId();
    }


    @Override
    public User getUserById(long id) {
        ThrowUtils.throwIF(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userDomainService.getById(id);
        ThrowUtils.throwIF(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    @Override
    public UserVO getUserVOById(long id) {
        return userDomainService.getUserVO(getUserById(id));
    }

    @Override
    public boolean deleteUser(DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return userDomainService.removeById(deleteRequest.getId());
    }

    @Override
    public void updateUser(User user) {
        boolean result = userDomainService.updateById(user);
        ThrowUtils.throwIF(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIF(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userDomainService.page(new Page<>(current, size),
                userDomainService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userDomainService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return userVOPage;
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userDomainService.listByIds(userIdSet);
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        return userDomainService.getEncryptPassword(userPassword);
    }

}




