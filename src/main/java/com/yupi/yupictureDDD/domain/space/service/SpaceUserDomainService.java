package com.yupi.yupictureDDD.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupictureDDD.domain.space.entity.SpaceUser;
import com.yupi.yupictureDDD.interfaces.dto.spaceuser.SpaceUserQueryRequest;

/**
 * @author leneve
 * @description 针对表【space_user】的数据库操作Service
 * @createDate 2025-08-25 09:51:14
 */
public interface SpaceUserDomainService  {

    /**
     * 获取查询条件(分页)
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * -------以上的方法都是各个表接口通用方法---
     */

    /**
     * 以上方法都是本接口都通用接口
     */

}
