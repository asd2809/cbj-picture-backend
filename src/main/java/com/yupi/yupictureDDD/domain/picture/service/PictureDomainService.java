package com.yupi.yupictureDDD.domain.picture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupictureDDD.domain.picture.entity.Picture;
import com.yupi.yupictureDDD.domain.user.entity.User;
import com.yupi.yupictureDDD.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupictureDDD.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.yupictureDDD.infrastructure.common.DeleteRequest;
import com.yupi.yupictureDDD.interfaces.dto.picture.*;
import com.yupi.yupictureDDD.interfaces.vo.picture.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author leneve
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-08-16 16:27:39
 */

public interface PictureDomainService {

    /**
     * 获得脱敏后的图片列表
     *
     * @param pictureList
     * @return
     */
    List<PictureVO> getPictureVOList(List<Picture> pictureList);

    /**
     * 获取脱敏后的图片
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);
    /**
     * 获取查询条件(分页)
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);


    //----------------------以上均为每个service必须的，至于脱敏后为什么没有使用就不清楚了--------------------------

    /**
     * 成功
     * 失败，直接抛异常
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);
    /**
     * 图片自动过审
     *
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 清理图片文件
     * @param  oldPicture
     */
    void clearPicture(Picture oldPicture);

    /**
     * 校验空间图片的权限
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser,Picture picture);


    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);


    /**
     * 删除图片
     * @param deleteRequest
     * @param request
     * @return
     */
    void deletePicture(DeleteRequest deleteRequest,HttpServletRequest request);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 编辑图片(用户)
     *
     * @param loginUser
     * @return
     */
    Picture editPicture(Picture picture, User loginUser);

    /**
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 批量修改图片
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPitureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * AI扩图
     *
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     */

    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);


    /**
     * 查询ai扩图任务
     * @param taskId
     * @return
     */
    GetOutPaintingTaskResponse getPicturePaintingTask(String taskId);
}
