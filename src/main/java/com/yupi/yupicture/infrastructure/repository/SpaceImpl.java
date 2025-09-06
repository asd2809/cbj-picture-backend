package com.yupi.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicture.domain.picture.entity.Picture;
import com.yupi.yupicture.domain.picture.repository.PictureRepository;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.space.repository.SpaceRepository;
import com.yupi.yupicture.infrastructure.mapper.PictureMapper;
import com.yupi.yupicture.infrastructure.mapper.SpaceMapper;
import org.springframework.stereotype.Service;

@Service
public class SpaceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceRepository {
}
