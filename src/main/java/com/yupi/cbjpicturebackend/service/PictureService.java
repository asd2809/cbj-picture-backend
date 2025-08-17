package com.yupi.cbjpicturebackend.service;

import com.yupi.cbjpicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.cbjpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import java.nio.channels.MulticastChannel;

/**
 * @author leneve
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-08-16 16:27:39
 */
public interface PictureService extends IService<Picture> {


    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);
}
