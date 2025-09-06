package com.yupi.yupicture.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicture.application.service.PictureApplicationService;
import com.yupi.yupicture.domain.picture.service.PictureDomainService;
import com.yupi.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicture.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.interfaces.dto.picture.*;
import com.yupi.yupicture.interfaces.vo.picture.PictureVO;
import com.yupi.yupicture.domain.picture.entity.Picture;
import com.yupi.yupicture.infrastructure.mapper.PictureMapper;
import com.yupi.yupicture.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author leneve
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-08-16 16:27:39
 */
@Slf4j
@Service
public class PictureApplicationServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureApplicationService {

    @Resource
    private PictureDomainService pictureDomainService;
    @Resource
    private UserApplicationService userApplicationService;

    /**
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
       return pictureDomainService.uploadPicture(inputSource, pictureUploadRequest, loginUser);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
       return pictureDomainService.getQueryWrapper(pictureQueryRequest);
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        return pictureDomainService.getPictureVO(picture, request);
    }

//    单独写一个方法,是为了把图片的userId添加到请求的图片中且是为了同一个用户多次中数据库中查询
//    而且把picture进行封装为vo(脱敏)
    /**
     * 分页查询获取图片封装
     *
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
//        对象列表=>封装对象列表
//        把picture转换成pictureVO
        List<PictureVO> pictureVOList = pictureList.stream()
//                .,ap(picture -> Picture.objTovo(picture))
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
// //1.关联查询用户信息
//将所有图片的userId放进一个set中,避免重复查询
//set类型可以自动去重,List不行
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
        //通过userId查询所有的用户
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //2.填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
//          是为了避免查不到这个用户
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUserVO(userApplicationService.getUserVO(user));
        });
//        转换分页对象
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void deletePicture(DeleteRequest deleteRequest, HttpServletRequest request) {
        pictureDomainService.deletePicture(deleteRequest, request);

    }


    @Override
    public void validPicture(Picture picture) {
        if (picture == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        picture.validPicture();
    }

    @Override
    public List<PictureVO> getPictureVOList(List<Picture> pictureList) {
        return pictureDomainService.getPictureVOList(pictureList);
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        pictureDomainService.doPictureReview(pictureReviewRequest, loginUser);
    }

    /**
     * 图片自动过审
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        pictureDomainService.fillReviewParams(picture, loginUser);
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
       return pictureDomainService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
    }

    @Override
    public Picture editPicture( Picture picture, User loginUser) {
        return pictureDomainService.editPicture(picture, loginUser);
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        return pictureDomainService.searchPictureByColor(spaceId, picColor, loginUser);
    }

    @Override
    public void editPitureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        pictureDomainService.editPitureByBatch(pictureEditByBatchRequest, loginUser);
    }

    /**
     * alk扩图
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        return pictureDomainService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
    }

    /**
     * 查询ai扩图的结果
     * @param taskId
     * @return
     */
    @Override
    public GetOutPaintingTaskResponse getPicturePaintingTask(String taskId) {
        return pictureDomainService.getPicturePaintingTask(taskId);
    }

    //实现异步
    @Async
    @Override
    public void clearPicture(Picture oldPicture) {
       pictureDomainService.clearPicture(oldPicture);

    }


    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        pictureDomainService.checkPictureAuth(loginUser, picture);
    }

}



