package com.yupi.yupictureDDD.domain.space.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupictureDDD.application.service.SpaceUserApplicationService;
import com.yupi.yupictureDDD.application.service.UserApplicationService;
import com.yupi.yupictureDDD.domain.space.entity.Space;
import com.yupi.yupictureDDD.domain.space.service.SpaceDomainService;
import com.yupi.yupictureDDD.domain.space.valueobject.SpaceLevelEnum;
import com.yupi.yupictureDDD.domain.user.entity.User;
import com.yupi.yupictureDDD.infrastructure.exception.BusinessException;
import com.yupi.yupictureDDD.infrastructure.exception.ErrorCode;
import com.yupi.yupictureDDD.infrastructure.exception.ThrowUtils;
import com.yupi.yupictureDDD.interfaces.dto.space.SpaceQueryRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;

/**
 * @author leneve
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-08-20 15:49:06
 */
@Service
public class SpaceDomainServiceImpl
        implements SpaceDomainService {

    @Resource
    private UserApplicationService userApplicationService;


    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;
    /**
     * 与事务有关
     *
     */
    @Resource
    private TransactionTemplate transactionTemplate;

//    @Resource
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIF(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR, "空间查询请求为空");
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Long userId = spaceQueryRequest.getUserId();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        /// 按照空间类型查询
        Integer spaceType = spaceQueryRequest.getSpaceType();
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq(ObjectUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        queryWrapper.like(ObjectUtil.isNotEmpty(spaceName), "spaceName", spaceName);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), "asecend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        //传入的时候检验空间级别是否存在
        ThrowUtils.throwIF(space.getSpaceLevel() == null, ErrorCode.PARAMS_ERROR, "需要提供空间级别");
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getSpaceLevelEnum(space.getSpaceLevel());
        if (spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        long maxCount = spaceLevelEnum.getMaxCount();
        long maxSize = spaceLevelEnum.getMaxSize();
        /**
         * 判断管理员是否自己设置了最大图片数量以及最大大小
         */
        if(space.getMaxCount() == null){
            space.setMaxCount(maxCount);
        }
        if(space.getMaxSize() == null){
            space.setMaxSize(maxSize);
        }
    }



    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        if (!space.getUserId().equals(loginUser.getId()) && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有该空间的权限");
        }
    }
}




