package com.yupi.yupictureDDD.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupictureDDD.domain.space.entity.SpaceUser;
import com.yupi.yupictureDDD.domain.space.repository.SpaceUserRepository;
import com.yupi.yupictureDDD.infrastructure.mapper.SpaceUserMapper;
import org.springframework.stereotype.Service;

@Service
public class SpaceUserRepositoryImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserRepository {
}
