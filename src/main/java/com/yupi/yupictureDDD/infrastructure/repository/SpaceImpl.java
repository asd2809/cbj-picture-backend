package com.yupi.yupictureDDD.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupictureDDD.domain.space.entity.Space;
import com.yupi.yupictureDDD.domain.space.repository.SpaceRepository;
import com.yupi.yupictureDDD.infrastructure.mapper.SpaceMapper;
import org.springframework.stereotype.Service;

@Service
public class SpaceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceRepository {
}
