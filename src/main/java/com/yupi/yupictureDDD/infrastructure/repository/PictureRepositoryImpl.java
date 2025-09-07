package com.yupi.yupictureDDD.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupictureDDD.domain.picture.entity.Picture;
import com.yupi.yupictureDDD.domain.picture.repository.PictureRepository;
import com.yupi.yupictureDDD.infrastructure.mapper.PictureMapper;
import org.springframework.stereotype.Service;

/**
 * 图片仓储实现
 */
@Service
public class PictureRepositoryImpl  extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {
}
