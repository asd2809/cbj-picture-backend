package com.yupi.yupicture.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.yupi.yupicture.interfaces.vo.space.SpaceUserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
