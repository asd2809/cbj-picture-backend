package com.yupi.yupictureDDD.domain.picture.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupictureDDD.domain.picture.entity.Picture;
import org.springframework.stereotype.Service;
/**
 * 图片仓储
 */
@Service
public interface PictureRepository  extends IService<Picture> {
}
