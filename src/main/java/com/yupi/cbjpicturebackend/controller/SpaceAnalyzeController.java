package com.yupi.cbjpicturebackend.controller;

import com.yupi.cbjpicturebackend.common.BaseResponse;
import com.yupi.cbjpicturebackend.common.ResultUtils;
import com.yupi.cbjpicturebackend.exception.ErrorCode;
import com.yupi.cbjpicturebackend.exception.ThrowUtils;
import com.yupi.cbjpicturebackend.model.dto.space.analyze.*;
import com.yupi.cbjpicturebackend.model.entity.Space;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.model.vo.space.anlyze.*;
import com.yupi.cbjpicturebackend.service.PictureService;
import com.yupi.cbjpicturebackend.service.SpaceAnalyzeService;
import com.yupi.cbjpicturebackend.service.SpaceService;
import com.yupi.cbjpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;


    /**
     *
     * @param spaceUsageAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceAnalyze(@RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
            HttpServletRequest request) {
        //校验参数
        ThrowUtils.throwIF(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        SpaceUsageAnalyzeResponse spaceUsageAnalyze = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, user);
        return ResultUtils.success(spaceUsageAnalyze);
    }

    /**
     * 获取空间图片分类分析
     * @param spaceCategoryRequest
     * @param request
     * @return
     */
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(@RequestBody
                                                                              SpaceCategoryRequest spaceCategoryRequest,
                                                                              HttpServletRequest request) {
        ThrowUtils.throwIF(spaceCategoryRequest == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyze = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryRequest, user);
        return ResultUtils.success(spaceCategoryAnalyze);
    }

    /**
     * 获取空间图片标签分析
     * @param spaceTagAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                                    HttpServletRequest request) {
        ThrowUtils.throwIF(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> spaceTageAnalyze = spaceAnalyzeService.getSpaceTageAnalyze(spaceTagAnalyzeRequest, user);
        return ResultUtils.success(spaceTageAnalyze);
    }

    /**
     * 获取空间图片大小分析
     * @param spaceSizeAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse> >getSpaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
                                                                      HttpServletRequest request) {
        ThrowUtils.throwIF(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeResponse> spaceSizeAnalyze = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, user);
        return ResultUtils.success(spaceSizeAnalyze);
    }

    /**
     * 获取空间图片用户行为分析
     * @param spaceUserAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,
                                                                      HttpServletRequest request) {
        ThrowUtils.throwIF(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        List<SpaceUserAnalyzeResponse> spaceUserAnalyze = spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, user);
        return ResultUtils.success(spaceUserAnalyze);
    }

    /**
     * 查询用户空间大小使用(仅管理员)
     * @param spaceRankAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/rank")
    public BaseResponse<List<Space>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
                                                         HttpServletRequest request) {
            ThrowUtils.throwIF(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
            User user = userService.getLoginUser(request);
        List<Space> spaceRankAnalyzes = spaceAnalyzeService.getSpaceRankAnalyzes(spaceRankAnalyzeRequest, user);
        return ResultUtils.success(spaceRankAnalyzes);
    }
}
