package com.yupi.yupictureDDD.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupictureDDD.interfaces.dto.space.SpaceAddRequest;
import com.yupi.yupictureDDD.interfaces.dto.space.SpaceQueryRequest;
import com.yupi.yupictureDDD.domain.space.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupictureDDD.domain.user.entity.User;
import com.yupi.yupictureDDD.interfaces.vo.space.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author leneve
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-08-20 15:49:06
 */

public interface SpaceApplicationService extends IService<Space> {

    /**
     * 空间数据校验
     * 要思考的是，在创建与更新空间的时候会对空间的数据进行校验
     * 比如创建空间的时候，空间名字与级别肯定要有
     * 但更新空间的时候不一定要有这俩
     * 通过添加一个add，来判断是创建还是更新
     *
     * @param space
     */
    void validSpace(Space space, boolean add);


    /**
     * 获取脱敏后的空间
     *
     * @param Space
     * @param
     * @return
     */
    SpaceVO getSpaceVO(Space Space);

    /**
     * 分页查询
     *
     * @param
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

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
     * 以上方法都是本接口都通用接口
     */

    /**
     * 创建空间接口
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验用户是否有该空间的权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);
}
