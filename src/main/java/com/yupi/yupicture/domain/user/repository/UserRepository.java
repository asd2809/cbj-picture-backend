package com.yupi.yupicture.domain.user.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicture.domain.user.entity.User;
import org.springframework.stereotype.Service;

/**
 * 用户仓储
 */
@Service
public interface UserRepository extends IService<User> {
}
