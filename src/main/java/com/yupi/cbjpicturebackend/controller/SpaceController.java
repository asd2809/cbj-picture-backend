package com.yupi.cbjpicturebackend.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.cbjpicturebackend.annotation.AuthCheck;
import com.yupi.cbjpicturebackend.common.BaseResponse;
import com.yupi.cbjpicturebackend.common.DeleteRequest;
import com.yupi.cbjpicturebackend.common.ResultUtils;
import com.yupi.cbjpicturebackend.constant.UserConstant;
import com.yupi.cbjpicturebackend.exception.ErrorCode;
import com.yupi.cbjpicturebackend.exception.ThrowUtils;
import com.yupi.cbjpicturebackend.model.dto.space.*;
import com.yupi.cbjpicturebackend.model.entity.Space;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.model.enums.SpaceLevelEnum;
import com.yupi.cbjpicturebackend.model.vo.SpaceVO;
import com.yupi.cbjpicturebackend.service.SpaceService;
import com.yupi.cbjpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request){
        //1.检验参数
        ThrowUtils.throwIF(spaceAddRequest==null,ErrorCode.PARAMS_ERROR,"请求参数错误");
        //2.获取当前用户状态
        User loginUser = userService.getLoginUser(request);
        //3.创建空间
        long result = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(result);
    }
    /**
     * 根据id删除空间
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(DeleteRequest deleteRequest,HttpServletRequest request){
//        服务类的代码应该写在这里的
        //1.判断传入的请求是否为空
        if (deleteRequest ==null || deleteRequest.getId() <= 0){
            ThrowUtils.throwIF(true,ErrorCode.PARAMS_ERROR);
        }
//        获取id
        //获取当前用户状态
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        //2.判断数据库中的图片是否存在
        Space space = spaceService.getById(id);
        ThrowUtils.throwIF(space == null,ErrorCode.PARAMS_ERROR);
        //仅本人或管理员可以删除
        spaceService.checkSpaceAuth(loginUser, space);
//        if(!userService.isAdmin(loginUser) ||!space.getUserId().equals(loginUser.getId()) ){
//            ThrowUtils.throwIF(true,ErrorCode.PARAMS_ERROR);
//        }
        //3.操作数据库
        boolean result = spaceService.removeById(id);
        ThrowUtils.throwIF(!result, ErrorCode.PARAMS_ERROR,"删除图片失败");

        return ResultUtils.success(result);
    }

    /**
     * 更新空间
     * @param spaceUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(SpaceUpdateRequest spaceUpdateRequest,
                                               HttpServletRequest request) {
        //1.判断传入的请求是否为空
        ThrowUtils.throwIF(spaceUpdateRequest==null || spaceUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR,"web请求的参数错误");

        //      2.操作数据库
        //把传入的请求对象转换为space
        Space space = spaceService.getById(spaceUpdateRequest.getId());
        BeanUtil.copyProperties(spaceUpdateRequest, space, CopyOptions.create().setIgnoreNullValue(true));
        //自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        //数据校验
        spaceService.validSpace(space,false);
        //先把请求的id通过数据库查询,是否存在这个空间
        Space oldSpace = spaceService.getById(spaceUpdateRequest.getId());
        ThrowUtils.throwIF(oldSpace == null,ErrorCode.PARAMS_ERROR,"该空间不存在数据库中");
        boolean result = spaceService.updateById(space);
        //这个才是真正的进行更新操作
        ThrowUtils.throwIF(!result,ErrorCode.SYSTEM_ERROR,"数据库操作失败");
        return ResultUtils.success(true);
    }
    /**
     * 管理员通过id获取空间
     * @param id
     * @return
     */
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(Long id){
        //1.判断请求是否为空
        ThrowUtils.throwIF(id==null,ErrorCode.PARAMS_ERROR,"传入空间的id为空");
        //2.操作数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIF(space==null,ErrorCode.SYSTEM_ERROR,"查不到这个空间");
        return ResultUtils.success(space);
    }

    /**
     * 根据id查询空间(封装类)
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(Long id){
        //1.判断请求是否为空
        ThrowUtils.throwIF(id==null,ErrorCode.PARAMS_ERROR,"传入空间的id为空");
        //2.操作数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIF(space==null,ErrorCode.SYSTEM_ERROR,"查不到这个空间");
        return ResultUtils.success(SpaceVO.objToVo(space));
    }

    /**
     * 分页查询空间(仅限管理员))
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest){
//       先判断请求是否为空
        ThrowUtils.throwIF(spaceQueryRequest==null,
                ErrorCode.PARAMS_ERROR,"请求参数错误");
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        //       操作数据库
        //进行分页查询
        Page<Space> spacePage = spaceService.page(new Page<>(current, pageSize),
                spaceService.getQueryWrapper(spaceQueryRequest));
        ThrowUtils.throwIF(spacePage==null,ErrorCode.SYSTEM_ERROR,"数据库操作失败");
        return ResultUtils.success(spacePage);
    }

    /**
     * 分页查询(封装类)
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                             HttpServletRequest request){
        //先判断请求是否为空
        ThrowUtils.throwIF(spaceQueryRequest == null,
                ErrorCode.PARAMS_ERROR,"请求参数错误");
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIF(pageSize > 20,ErrorCode.PARAMS_ERROR);
        //       操作数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, pageSize),
                spaceService.getQueryWrapper(spaceQueryRequest));
        ThrowUtils.throwIF(spacePage==null,ErrorCode.SYSTEM_ERROR,"数据库操作失败");
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage,request));
    }
    /**
     * 编辑空间(主要是用户使用)
     * @param spaceEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Space> editSpace(SpaceEditRequest spaceEditRequest,HttpServletRequest request){
        //判断请求是否为空
        ThrowUtils.throwIF(spaceEditRequest == null || spaceEditRequest.getId() <= 0,ErrorCode.PARAMS_ERROR,"web传入的参数错误");
        //数据库操作，先通过传入的id获取数据库中对应的space表
        Space space = spaceService.getById(spaceEditRequest.getId());
        //这个工具可以保证，当spaceEditRequest传入的属性为空的时候，不会赋值给space
        BeanUtil.copyProperties(spaceEditRequest, space, CopyOptions.create().setIgnoreNullValue(true));
        //设置编辑时间
        space.setEditTime(new Date());
        //自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        //数据校验
        spaceService.validSpace(space,false);
        User loginUser = userService.getLoginUser(request);
         //判断是否存在
            long id = spaceEditRequest.getId();
        Space oldSpace = spaceService.getById(id);
//      仅本人或管理员可以编辑
        spaceService.checkSpaceAuth(loginUser, oldSpace);

        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIF(!result,ErrorCode.SYSTEM_ERROR,"数据库操作失败");

        return ResultUtils.success(space);
    }


    /**
     * 获取所有空间级别
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel(){
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                ))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }





}
