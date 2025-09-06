package com.yupi.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.domain.user.repository.UserRepository;
import com.yupi.yupicture.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 用户仓促实现
 */
@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper,User> implements UserRepository {
}
