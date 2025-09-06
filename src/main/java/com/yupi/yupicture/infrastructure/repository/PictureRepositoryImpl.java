package com.yupi.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicture.domain.picture.entity.Picture;
import com.yupi.yupicture.domain.picture.repository.PictureRepository;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.domain.user.repository.UserRepository;
import com.yupi.yupicture.infrastructure.mapper.PictureMapper;
import com.yupi.yupicture.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 图片仓储实现
 */
@Service
public class PictureRepositoryImpl  extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {
}
