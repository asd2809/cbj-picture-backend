package com.yupi.yupictureDDD.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupictureDDD.domain.space.entity.Space;
import com.yupi.yupictureDDD.domain.user.entity.User;
import com.yupi.yupictureDDD.interfaces.dto.space.SpaceQueryRequest;

/**
 * @author leneve
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-08-20 15:49:06
 */

public interface SpaceDomainService  {

    /**
     * 获取查询条件(分页)
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * -------以上的方法都是各个表接口通用方法---
     */

    /**
     * 根据space填充空间级别
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);


    /**
     * 校验用户是否有该空间的权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);
}
