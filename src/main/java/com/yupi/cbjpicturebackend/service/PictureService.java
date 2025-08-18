package com.yupi.cbjpicturebackend.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.cbjpicturebackend.common.BaseResponse;
import com.yupi.cbjpicturebackend.common.DeleteRequest;
import com.yupi.cbjpicturebackend.model.dto.picture.PictureQueryRequest;
import com.yupi.cbjpicturebackend.model.dto.picture.PictureReviewRequest;
import com.yupi.cbjpicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.cbjpicturebackend.model.dto.user.UserQueryRequest;
import com.yupi.cbjpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.model.vo.PictureVO;
import com.yupi.cbjpicturebackend.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.nio.channels.MulticastChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author leneve
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-08-16 16:27:39
 */
public interface PictureService extends IService<Picture> {


    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 获取查询条件
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);


    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    //    分页查询
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 删除图片
     * @param deleteRequest
     * @param request
     * @return
     */
    BaseResponse<Boolean> deletePicture(DeleteRequest deleteRequest,HttpServletRequest request);

    /**
     * 图片数据校验
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 获得脱敏后的图片列表
     * @param pictureList
     * @return
     */
    List<PictureVO> getPictureVOList(List<Picture> pictureList);

    /**
     * 成功
     * 失败，直接抛异常
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);
}
