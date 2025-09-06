package com.yupi.yupicture.application.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicture.application.service.UserApplicationService;
import com.yupi.yupicture.domain.space.service.SpaceUserDomainService;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.domain.space.valueobject.SpaceRoleEnum;
import com.yupi.yupicture.interfaces.vo.space.SpaceUserVo;
import com.yupi.yupicture.interfaces.vo.space.SpaceVO;
import com.yupi.yupicture.interfaces.vo.user.UserVO;
import com.yupi.yupicture.application.service.SpaceApplicationService;
import com.yupi.yupicture.application.service.SpaceUserApplicationService;
import com.yupi.yupicture.infrastructure.mapper.SpaceUserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author leneve
 * @description 针对表【space_user】的数据库操作Service实现
 * @createDate 2025-08-25 09:51:14
 */
@Service
public class SpaceUserApplicationServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserApplicationService {


    @Resource
    @Lazy
    private SpaceApplicationService spaceApplicationService;
    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private SpaceUserDomainService spaceUserDomainService;

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest,User loginUser) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        //校验参数
        validSpaceUser(spaceUser, true);
        //添加新数据
        boolean save = this.save(spaceUser);
        ThrowUtils.throwIF(!save, ErrorCode.OPERATION_ERROR, "向数据库添加数据失败");
        return spaceUser.getId();
    }

    @Override
    public List<SpaceUserVo> getSpaceUserVOList(List<SpaceUser> spaceUsersList) {
        if (ObjUtil.isEmpty(spaceUsersList)) {
            return new ArrayList<>();
        }
        /**
         *因为要通过userId与spaceId获取对应的user与space,
         *如果一个元素查询一次数据库会导致查询数据库频繁
         * 而不同的角色的spaceId可能胡一样的
         * 以及userId也可能会一样，可以先把所有spaceId与userId查询出来通过Set形式存放
         * 然后再统一查询user与space
         */
        //获取所有的userId
        Set<Long> userIdSet = spaceUsersList.stream()
                .map(SpaceUser::getUserId)
                .collect(Collectors.toSet());
        //获取所有的spaceId
        Set<Long> spaceIdSet = spaceUsersList.stream()
                .map(SpaceUser::getSpaceId)
                .collect(Collectors.toSet());

        //向数据库查询得到的user
        List<User> userList = userApplicationService.listByIds(userIdSet);
        //向数据库中查询得到的space
        List<Space> spacesList = spaceApplicationService.listByIds(spaceIdSet);

        //将获取的List转换为map,方便后续通过id直接获取
        Map<Long, User> userMap = userList.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Space> spaceMap = spacesList.stream()
                .collect(Collectors.toMap(Space::getId, space -> space));
        //开始批量的将对象转换为封装类
        return spaceUsersList.stream()
                .map(spaceUser -> {
                    SpaceUserVo spaceUserVo = SpaceUserVo.objToVo(spaceUser);

                    User user = userMap.get(spaceUser.getUserId());
                    if (user != null) {
                        spaceUserVo.setUserVO(userApplicationService.getUserVO(user));
                    }
                    Space space = spaceMap.get(spaceUser.getSpaceId());
                    if (space != null) {
                        spaceUserVo.setSpaceVO(spaceApplicationService.getSpaceVO(space));
                        // 同时设置原始space字段以兼容前端代码
                        spaceUserVo.setSpace(space);
                    }

                    return spaceUserVo;
                })
                .collect(Collectors.toList());
    }


    /**
     * 空间数据校验
     * 要思考的是，在创建与更新空间的时候会对空间的数据进行校验
     * 比如创建空间的时候，空间名字与级别肯定要有
     * 但更新空间的时候不一定要有这俩
     * 通过添加一个add，来判断是创建还是更新
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIF(spaceUser == null, ErrorCode.PARAMS_ERROR);
        //创建的时候，用户id与空间id必须填写
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIF(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userApplicationService.getUserById(userId);
            ThrowUtils.throwIF(user == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIF(space == null, ErrorCode.PARAMS_ERROR);
        }
        //默认空间角色为浏览者
        spaceUser.setSpaceRole(SpaceRoleEnum.VIEWER.getValue());
    }

    @Override
    public SpaceUserVo getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        ThrowUtils.throwIF(spaceUser == null, ErrorCode.PARAMS_ERROR);
        //对象转封装类
        SpaceUserVo spaceUserVo = SpaceUserVo.objToVo(spaceUser);
        //需要获取对应的spaceVO与userVo封装进去
        Long spaceId = spaceUser.getSpaceId();

        if (spaceId != null && spaceId >= 0) {
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIF(space == null, ErrorCode.PARAMS_ERROR);
            SpaceVO spaceVO = spaceApplicationService.getSpaceVO(space);
            spaceUserVo.setSpaceVO(spaceVO);
        }

        Long userId = spaceUser.getUserId();
        if (userId != null && userId >= 0) {
            User user = userApplicationService.getUserById(userId);
            ThrowUtils.throwIF(user == null, ErrorCode.PARAMS_ERROR);
            UserVO userVO = userApplicationService.getUserVO(user);
            spaceUserVo.setUserVO(userVO);
        }


        return spaceUserVo;
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }


}




