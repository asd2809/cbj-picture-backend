package com.yupi.yupicture.interfaces.controller;

import com.yupi.yupicture.application.service.PictureApplicationService;
import com.yupi.yupicture.application.service.UserApplicationService;
import com.yupi.yupicture.infrastructure.common.BaseResponse;
import com.yupi.yupicture.infrastructure.common.ResultUtils;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.application.service.SpaceAnalyzeApplicationService;
import com.yupi.yupicture.application.service.SpaceApplicationService;
import com.yupi.yupicture.interfaces.dto.space.analyze.*;
import com.yupi.yupicture.interfaces.vo.space.anlyze.*;
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
    private SpaceAnalyzeApplicationService spaceAnalyzeApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

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
        User user = userApplicationService.getLoginUser(request);
        SpaceUsageAnalyzeResponse spaceUsageAnalyze = spaceAnalyzeApplicationService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, user);
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
        User user = userApplicationService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyze = spaceAnalyzeApplicationService.getSpaceCategoryAnalyze(spaceCategoryRequest, user);
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
        User user = userApplicationService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> spaceTageAnalyze = spaceAnalyzeApplicationService.getSpaceTageAnalyze(spaceTagAnalyzeRequest, user);
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
        User user = userApplicationService.getLoginUser(request);
        List<SpaceSizeAnalyzeResponse> spaceSizeAnalyze = spaceAnalyzeApplicationService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, user);
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
        User user = userApplicationService.getLoginUser(request);
        List<SpaceUserAnalyzeResponse> spaceUserAnalyze = spaceAnalyzeApplicationService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, user);
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
        User user = userApplicationService.getLoginUser(request);
        List<Space> spaceRankAnalyzes = spaceAnalyzeApplicationService.getSpaceRankAnalyzes(spaceRankAnalyzeRequest, user);
        return ResultUtils.success(spaceRankAnalyzes);
    }
}
