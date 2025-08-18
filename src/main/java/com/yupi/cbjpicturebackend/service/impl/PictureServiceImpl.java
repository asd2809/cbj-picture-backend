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
import com.yupi.cbjpicturebackend.exception.BusinessException;
import com.yupi.cbjpicturebackend.exception.ErrorCode;
import com.yupi.cbjpicturebackend.exception.ThrowUtils;
import com.yupi.cbjpicturebackend.manager.upload.PictureUpload;
import com.yupi.cbjpicturebackend.manager.upload.PictureUploadTemplate;
import com.yupi.cbjpicturebackend.manager.upload.UrlPictureUpload;
import com.yupi.cbjpicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.cbjpicturebackend.model.dto.picture.*;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.model.enums.PictureReviewStatusEnum;
import com.yupi.cbjpicturebackend.model.vo.PictureVO;
import com.yupi.cbjpicturebackend.model.vo.UserVO;
import com.yupi.cbjpicturebackend.service.PictureService;
import com.yupi.cbjpicturebackend.model.entity.Picture;
import com.yupi.cbjpicturebackend.mapper.PictureMapper;
import com.yupi.cbjpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.print.Doc;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author leneve
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-08-16 16:27:39
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Autowired
    private UserService userService;
    @Resource
    private PictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;


    /**
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
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
//
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIF(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
//           仅限本人或管理员可以编辑图片
            if (oldPicture.getUserId().equals(loginUser.getId()) || !userService.isAdmin(loginUser)) {
                ThrowUtils.throwIF(true, ErrorCode.NO_AUTH_ERROR);
            }
        }
        //4.上传图片
        //按照用户id划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        //根据 InputSource 的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        Picture picture = new Picture();
        //BeanUtils.copyProperties(uploadPictureResult, picture);,不使用BeanUtil的原因是两者字段名存在不一样的
        picture.setUrl(uploadPictureResult.getUrl());
        //支持外层传递图片名称
        String picName = uploadPictureResult.getName();
        if(pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId()); // ✅ 补充用户ID
//        补充过审参数
        this.fillReviewParams(picture, loginUser);
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
        Double picScale = pictureQueryRequest.getPicScale();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String picFormat = pictureQueryRequest.getPicFormat();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewUserId = pictureQueryRequest.getReviewUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

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
        queryWrapper.like(StrUtil.isNotEmpty(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(searchText), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(searchText), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(sortField), "sortField", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus),"reviewStatus",reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewUserId), "reviewUserId", reviewUserId);
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
        Picture picture = this.getById(id);
        ThrowUtils.throwIF(picture == null,ErrorCode.PARAMS_ERROR);
        //仅本人或管理员可以删除
        if(!picture.getUserId().equals(loginUser.getId() )|| !userService.isAdmin(loginUser)){
            ThrowUtils.throwIF(true,ErrorCode.PARAMS_ERROR);
        }
        //3.操作数据库
        boolean result = this.removeById(id);
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

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //1.检验参数
        ThrowUtils.throwIF(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();

        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            ThrowUtils.throwIF(true, ErrorCode.PARAMS_ERROR);
        }
        //2.判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIF(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //3.检验审核状态是否重复,已经是想要修改的状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            ThrowUtils.throwIF(true, ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        //4.数据库操作
//        创建新的picture是为了只更新需要更新的字段
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(oldPicture, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewMessage(reviewMessage);

        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIF(!result, ErrorCode.OPERATION_ERROR, "数据库操作失败");
    }

    /**
     * 图片自动过审
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
//            管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
//            非管理员上传图片的状态都是待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());

        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //1.校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIF(count > 30, ErrorCode.PARAMS_ERROR, "最多抓取30条");
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if(StrUtil.isBlank(namePrefix)){
            //如果传递的请求的图片名为空，则默认为searchText(关键词)
            namePrefix = searchText;
        }
        //2.抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document = null;
        try {

            document = Jsoup.connect(fetchUrl).get();
        } catch (Exception e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "获取页面失败");
        }
        //3.解析内容
        //获取第一个最外层，可以用来判断是否有这个html
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        //遍历元素依次上传元素
        int uploadCount = 0;
        for (Element element : imgElementList) {
            //获取图片中的url
            String fileUrl = element.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过{}", fileUrl);
                continue;
            }
            //处理图片的地址，防止转义又或者和对象存储冲突的问题
            //codefath.cn?yupi=adadss  改为codefather.cn
            //把url后面的参数全部删除
            int questMarkIndex = fileUrl.indexOf("?");
            //没找到questMarkIndex为-1
            if (questMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questMarkIndex);
            }
            //4.上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功,id = {}", pictureVO.getId());
                uploadCount++;
            }catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if(uploadCount >= count){{
                break;
            }}
        }
        return uploadCount;
    }


}




