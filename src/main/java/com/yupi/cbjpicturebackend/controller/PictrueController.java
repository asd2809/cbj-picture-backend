package com.yupi.cbjpicturebackend.controller;


import com.yupi.cbjpicturebackend.annotation.AuthCheck;
import com.yupi.cbjpicturebackend.common.BaseResponse;
import com.yupi.cbjpicturebackend.common.ResultUtils;
import com.yupi.cbjpicturebackend.constant.UserConstant;
import com.yupi.cbjpicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.cbjpicturebackend.model.entity.Picture;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.model.vo.PictureVO;
import com.yupi.cbjpicturebackend.service.PictureService;
import com.yupi.cbjpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictrueController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> upload(@RequestParam("file") MultipartFile multipartFile,
                                          PictureUploadRequest pictureUploadRequest,
                                          HttpServletRequest request
                                          ) {
        User LoginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile,pictureUploadRequest,LoginUser);
        return ResultUtils.success(pictureVO);
    }

}
