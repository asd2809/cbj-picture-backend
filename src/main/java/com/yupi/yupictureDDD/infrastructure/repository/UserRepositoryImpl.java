package com.yupi.yupictureDDD.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupictureDDD.domain.user.entity.User;
import com.yupi.yupictureDDD.domain.user.repository.UserRepository;
import com.yupi.yupictureDDD.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 用户仓促实现
 */
@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper,User> implements UserRepository {
}
