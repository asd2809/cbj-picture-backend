package com.yupi.yupictureDDD.interfaces.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupictureDDD.infrastructure.common.BaseResponse;
import com.yupi.yupictureDDD.infrastructure.common.DeleteRequest;
import com.yupi.yupictureDDD.infrastructure.common.ResultUtils;
import com.yupi.yupictureDDD.infrastructure.exception.BusinessException;
import com.yupi.yupictureDDD.infrastructure.exception.ErrorCode;
import com.yupi.yupictureDDD.infrastructure.exception.ThrowUtils;
import com.yupi.yupictureDDD.shared.auth.annotation.SaSpaceCheckPermission;
import com.yupi.yupictureDDD.shared.auth.model.SpaceUserPermissionConstant;
import com.yupi.yupictureDDD.interfaces.Assembler.SpaceUserAssembler;
import com.yupi.yupictureDDD.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.yupi.yupictureDDD.interfaces.dto.spaceuser.SpaceUserEditRequest;
import com.yupi.yupictureDDD.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.yupi.yupictureDDD.domain.space.entity.SpaceUser;
import com.yupi.yupictureDDD.domain.user.entity.User;
import com.yupi.yupictureDDD.interfaces.vo.space.SpaceUserVo;
import com.yupi.yupictureDDD.application.service.SpaceUserApplicationService;
import com.yupi.yupictureDDD.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 添加团队空间
     *
     * @param spaceUserAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest,
                                           HttpServletRequest request) {
        ThrowUtils.throwIF(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = userApplicationService.getLoginUser(request);
        Long result = spaceUserApplicationService.addSpaceUser(spaceUserAddRequest,user);
        return ResultUtils.success(result);
    }
    /**
     * 删除团队空间
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIF(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        //判断是否存
        SpaceUser spaceUser = spaceUserApplicationService.getById(id);
        ThrowUtils.throwIF(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        //执行删除操作
        boolean result = spaceUserApplicationService.removeById(id);
        ThrowUtils.throwIF(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        //参数校验
        ThrowUtils.throwIF(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIF(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.NOT_FOUND_ERROR);
        //查询数据库
        //getOne()表示通过QueryWrapper条件构造器只查询一条数据
        SpaceUser spaceUser = spaceUserApplicationService.getOne(spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIF(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询空间用户列表
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVo>> listSpaceUserVo(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIF(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        //获取条件构造器
        QueryWrapper<SpaceUser> queryWrapper = spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest);
        //从数据库中查询数据
        List<SpaceUser> list = spaceUserApplicationService.list(queryWrapper);
        //对数据列表进行脱敏
        return ResultUtils.success(spaceUserApplicationService.getSpaceUserVOList(list));
    }

    /**
     * 编辑团队空间成员信息
     *
     * @param spaceUserEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> editSpaceUser(SpaceUserEditRequest spaceUserEditRequest, HttpServletRequest request) {
        if (spaceUserEditRequest == null || spaceUserEditRequest.getSpaceUserId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //将实体类和DTO进行转换
        SpaceUser spaceUser = SpaceUserAssembler.toSpaceUserEntity(spaceUserEditRequest);
        //
        spaceUserApplicationService.validSpaceUser(spaceUser, false);
        //判断是否存在
        Long spaceUserId = spaceUserEditRequest.getSpaceUserId();
        SpaceUser oldSpaceUser = spaceUserApplicationService.getById(spaceUserId);
        ThrowUtils.throwIF(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        //更新数据
        boolean save = spaceUserApplicationService.save(oldSpaceUser);
        ThrowUtils.throwIF(!save, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询我加入的团队空间列表(可以通过当前登录的用户id进行查询,)
     * @param request
     * @return
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVo>> listMyTeamSpace(HttpServletRequest request) {
        //查询是通过当前用户进行查询
        User longinUser = userApplicationService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        //只需要对条件构造器添加一个userId即可,其他条件不需要管
        spaceUserQueryRequest.setUserId(longinUser.getId());
        List<SpaceUser> list = spaceUserApplicationService.list(spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest));

        return ResultUtils.success(spaceUserApplicationService.getSpaceUserVOList(list));
    }
}
