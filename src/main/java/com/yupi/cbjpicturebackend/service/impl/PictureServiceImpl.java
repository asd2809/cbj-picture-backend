package com.yupi.cbjpicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.cbjpicturebackend.exception.ErrorCode;
import com.yupi.cbjpicturebackend.exception.ThrowUtils;
import com.yupi.cbjpicturebackend.manager.FileManager;
import com.yupi.cbjpicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.cbjpicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.model.vo.PictureVO;
import com.yupi.cbjpicturebackend.service.PictureService;
import com.yupi.cbjpicturebackend.model.entity.Picture;
import com.yupi.cbjpicturebackend.mapper.PictureMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.nio.channels.MulticastChannel;
import java.util.Collection;
import java.util.Date;

/**
 * @author leneve
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-08-16 16:27:39
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private FileManager fileManager;

    /**
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIF(loginUser == null, ErrorCode.PARAMS_ERROR, "用户未登录");
        //2.判断是新增还是更新
        Long pictureId = null;
        if (pictureUploadRequest != null) {
//            更新
            pictureId = pictureUploadRequest.getId();
        }
        //3.如果是更新，判断图片是否存在
        if (pictureId != null) {
            boolean exists = this.lambdaQuery().eq(Picture::getId, pictureId).exists();
            ThrowUtils.throwIF(exists, ErrorCode.PARAMS_ERROR, "图片不存在");
        }
        //4.上传图片
        //按照用户id划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);

        Picture picture = new Picture();
        //BeanUtils.copyProperties(uploadPictureResult, picture);,不使用BeanUtil的原因是两者字段名存在不一样的
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId()); // ✅ 补充用户ID
        //5.保存到数据库
        //如果PictureId不为空，则为更新
        //反之为新增
        if (pictureId != null) {
        //如果是更新，补充id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
//        如果是新增
//        这个方法既可以进行更新也可以对数据库进行插入
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIF(!result, ErrorCode.PARAMS_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);

    }
}




