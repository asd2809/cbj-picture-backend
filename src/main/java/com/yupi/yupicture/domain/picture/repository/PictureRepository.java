package com.yupi.yupicture.domain.picture.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicture.domain.picture.entity.Picture;
import com.yupi.yupicture.domain.user.entity.User;
import org.springframework.stereotype.Service;
/**
 * 图片仓储
 */
@Service
public interface PictureRepository  extends IService<Picture> {
}
