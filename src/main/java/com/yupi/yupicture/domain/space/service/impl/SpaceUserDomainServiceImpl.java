package com.yupi.yupicture.domain.space.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicture.application.service.SpaceApplicationService;
import com.yupi.yupicture.application.service.SpaceUserApplicationService;
import com.yupi.yupicture.application.service.UserApplicationService;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.domain.space.service.SpaceUserDomainService;
import com.yupi.yupicture.domain.space.valueobject.SpaceRoleEnum;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.infrastructure.mapper.SpaceUserMapper;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.yupi.yupicture.interfaces.vo.space.SpaceUserVo;
import com.yupi.yupicture.interfaces.vo.space.SpaceVO;
import com.yupi.yupicture.interfaces.vo.user.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author leneve
 * @description 针对表【space_user】的数据库操作Service实现
 * @createDate 2025-08-25 09:51:14
 */
@Service
public class SpaceUserDomainServiceImpl
        implements SpaceUserDomainService {
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




