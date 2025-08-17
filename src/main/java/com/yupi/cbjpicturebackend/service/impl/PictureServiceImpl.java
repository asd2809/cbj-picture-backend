package com.yupi.cbjpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.cbjpicturebackend.common.BaseResponse;
import com.yupi.cbjpicturebackend.common.DeleteRequest;
import com.yupi.cbjpicturebackend.common.ResultUtils;
import com.yupi.cbjpicturebackend.constant.UserConstant;
import com.yupi.cbjpicturebackend.exception.BusinessException;
import com.yupi.cbjpicturebackend.exception.ErrorCode;
import com.yupi.cbjpicturebackend.exception.ThrowUtils;
import com.yupi.cbjpicturebackend.manager.FileManager;
import com.yupi.cbjpicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.cbjpicturebackend.model.dto.picture.PictureQueryRequest;
import com.yupi.cbjpicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.model.vo.PictureVO;
import com.yupi.cbjpicturebackend.model.vo.UserVO;
import com.yupi.cbjpicturebackend.service.PictureService;
import com.yupi.cbjpicturebackend.model.entity.Picture;
import com.yupi.cbjpicturebackend.mapper.PictureMapper;
import com.yupi.cbjpicturebackend.service.UserService;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.print.Pageable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author leneve
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-08-16 16:27:39
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private FileManager fileManager;
    @Autowired
    private UserService userService;
    @Autowired
    private PictureService pictureService;

    /**
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIF(loginUser == null, ErrorCode.PARAMS_ERROR, "用户未登录");
        //2.判断是新增还是更新
        Long pictureId = null;
        if (pictureUploadRequest != null) {
//            更新
            pictureId = pictureUploadRequest.getId();
        }
        //3.如果是更新，判断图片是否存在
        if (pictureId != null) {
            boolean exists = this.lambdaQuery().eq(Picture::getId, pictureId).exists();
            ThrowUtils.throwIF(exists, ErrorCode.PARAMS_ERROR, "图片不存在");
        }
        //4.上传图片
        //按照用户id划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);

        Picture picture = new Picture();
        //BeanUtils.copyProperties(uploadPictureResult, picture);,不使用BeanUtil的原因是两者字段名存在不一样的
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId()); // ✅ 补充用户ID
        //5.保存到数据库
        //如果PictureId不为空，则为更新
        //反之为新增
        if (pictureId != null) {
            //如果是更新，补充id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
//        如果是新增
//        这个方法既可以进行更新也可以对数据库进行插入
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIF(!result, ErrorCode.PARAMS_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);

    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String picFormat = pictureQueryRequest.getPicFormat();

        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Double picScale = pictureQueryRequest.getPicScale();
//        从多字段中搜索
        if (StrUtil.isNotEmpty(searchText)) {
//            需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }

//        第一个参数是判断该id是否为空
        queryWrapper.eq(ObjUtil.isEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isEmpty(name), "name", name);
        queryWrapper.like(StrUtil.isNotEmpty(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotEmpty(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotEmpty(searchText), "category", category);
        queryWrapper.eq(StrUtil.isNotEmpty(sortField), "picWidth", picWidth);
        queryWrapper.eq(StrUtil.isNotEmpty(sortOrder), "picHeight", picHeight);
        queryWrapper.eq(StrUtil.isNotEmpty(searchText), "picSize", picSize);
        queryWrapper.eq(StrUtil.isNotEmpty(sortField), "sortField", picScale);
//        JSON数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        //        排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
//        对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
//        关联查询用户信息
        Long userId = pictureVO.getUserId();
        if (userId == null || userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUserVO(userVO);
        }
        return pictureVO;
    }

//    单独写一个方法,是为了把图片的userId添加到请求的图片中且是为了同一个用户多次中数据库中查询
//    而且把picture进行封装为vo(脱敏)
    /**
     * 分页查询获取图片封装
     *
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
//        对象列表=>封装对象列表
//        把picture转换成pictureVO
        List<PictureVO> pictureVOList = pictureList.stream()
//                .,ap(picture -> Picture.objTovo(picture))
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
//      //1.关联查询用户信息
//        将所有图片的userId放进一个set中,避免重复查询
//        set类型可以自动去重,List不行
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
//        通过userId查询所有的用户
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
//      2.填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
//          是为了避免查不到这个用户
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUserVO(userService.getUserVO(user));
        });
//        转换分页对象
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public BaseResponse<Boolean> deletePicture(DeleteRequest deleteRequest, HttpServletRequest request) {
        //1.判断传入的请求是否为空
        if (deleteRequest ==null || deleteRequest.getId() <= 0){
            ThrowUtils.throwIF(true,ErrorCode.PARAMS_ERROR);
        }
//        获取id
        //获取当前用户状态
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        //2.判断数据库中的图片是否存在
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIF(picture == null,ErrorCode.PARAMS_ERROR);
        //仅本人或管理员可以删除
        if(!picture.getUserId().equals(loginUser.getId() )|| !userService.isAdmin(loginUser)){
            ThrowUtils.throwIF(true,ErrorCode.PARAMS_ERROR);
        }
        //3.操作数据库
        boolean result = pictureService.removeById(id);
        ThrowUtils.throwIF(!result, ErrorCode.PARAMS_ERROR,"删除图片失败");
        return ResultUtils.success(result);
    }
    /**
     * 数据校验
     *
     * @param picture
     */

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIF(picture == null, ErrorCode.PARAMS_ERROR, "传入的图片为空");
//      从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
//        修改数据,id不能为空,有参数校验
        ThrowUtils.throwIF(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id不能为空");
        if(StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIF(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        if(StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIF(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public List<PictureVO> getPictureVOList(List<Picture> pictureList) {
//        首先判断传来的列表是否为空
        if(CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
//        使用流式输出
        return pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }


}




