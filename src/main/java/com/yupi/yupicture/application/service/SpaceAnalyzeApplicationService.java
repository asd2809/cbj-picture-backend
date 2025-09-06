package com.yupi.yupicture.application.service;


import com.baomidou.mybatisplus.extension.service.IService;

import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.interfaces.dto.space.analyze.*;
import com.yupi.yupicture.interfaces.vo.space.anlyze.*;

import java.util.List;

/**
 * 针对表(space 空间)的数据库操作Service
 */
public interface SpaceAnalyzeApplicationService {

    /**
     * 获取空间使用情况分析
     *
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 空间分类分析
     *
     * @param spaceCategoryRequest
     * @param loginUser
     * @return
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryRequest spaceCategoryRequest, User loginUser);

    /**
     * 空间标签分析
     *
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceTagAnalyzeResponse> getSpaceTageAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 空间大小分析
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);


    /**
     * 空间用户上传行为分析响应
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceUserAnalyzeResponse>  getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    List<Space> getSpaceRankAnalyzes(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);

}
