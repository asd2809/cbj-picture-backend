package com.yupi.cbjpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.cbjpicturebackend.api.aliyunai.AliYunAiApi;
import com.yupi.cbjpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.cbjpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.cbjpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.cbjpicturebackend.common.DeleteRequest;
import com.yupi.cbjpicturebackend.exception.BusinessException;
import com.yupi.cbjpicturebackend.exception.ErrorCode;
import com.yupi.cbjpicturebackend.exception.ThrowUtils;
import com.yupi.cbjpicturebackend.manager.CosManager;
import com.yupi.cbjpicturebackend.manager.upload.PictureUpload;
import com.yupi.cbjpicturebackend.manager.upload.PictureUploadTemplate;
import com.yupi.cbjpicturebackend.manager.upload.UrlPictureUpload;
import com.yupi.cbjpicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.cbjpicturebackend.model.dto.picture.*;
import com.yupi.cbjpicturebackend.model.entity.Space;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.model.enums.PictureReviewStatusEnum;
import com.yupi.cbjpicturebackend.model.enums.SpaceTypeEnum;
import com.yupi.cbjpicturebackend.model.vo.PictureVO;
import com.yupi.cbjpicturebackend.model.vo.UserVO;
import com.yupi.cbjpicturebackend.service.PictureService;
import com.yupi.cbjpicturebackend.model.entity.Picture;
import com.yupi.cbjpicturebackend.mapper.PictureMapper;
import com.yupi.cbjpicturebackend.service.SpaceService;
import com.yupi.cbjpicturebackend.service.UserService;
import com.yupi.cbjpicturebackend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.util.*;
import java.util.List;
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

    @Resource
    private UserService userService;
    @Resource
    private PictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private SpaceService spaceService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Autowired
    private CosManager cosManager;
    @Autowired
    private AliYunAiApi aliYunAiApi;
    /**
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIF(loginUser == null, ErrorCode.PARAMS_ERROR, "用户未登录");
        //1.1校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIF(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            /// 虽然有了Sa-Token，但对于整体的用户的权限还是需要验证的
            Integer spaceType = space.getSpaceType();
            //必须是该用户管理员才能上传
            if (!space.getUserId().equals(loginUser.getId()) && SpaceTypeEnum.TEAM.getValue() != spaceType ) {
                throw new BusinessException( ErrorCode.NO_AUTH_ERROR, "没有向该空间上传图片的权限");
            }
            //1.2校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException( ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException( ErrorCode.OPERATION_ERROR, "空间图片大小不足");
            }
        }
        //2.判断是新增还是更新
        Long pictureId = null;
        if (pictureUploadRequest != null) {
//            更新
            pictureId = pictureUploadRequest.getId();
        }

        //3.如果是更新，判断图片是否存在
        //把旧图片放在外面，方方便删除
        Picture oldPicture;
        if (pictureId != null) {
            oldPicture = this.getById(pictureId);
            ThrowUtils.throwIF(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            //仅限本人或管理员可以编辑图片
            if (oldPicture.getUserId().equals(loginUser.getId())  && !userService.isAdmin(loginUser)) {
                ThrowUtils.throwIF(true, ErrorCode.NO_AUTH_ERROR);
            }
            //校验空间是否一致
            //分为，没传spaceId与传了spaceId
            if(spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            }else{
                //校验两次spaceId是否一样
                if (!oldPicture.getSpaceId().equals(spaceId)) {
                    throw new BusinessException( ErrorCode.PARAMS_ERROR, "空间id不一样");
                }
            }

        } else {
            oldPicture = null;
        }
        //4.上传图片
        //按照用户id划分目录 =>私有图片划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            //公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        }else{
            //私有空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        //根据 InputSource 的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        Picture picture = new Picture();
        //BeanUtils.copyProperties(uploadPictureResult, picture);,不使用BeanUtil的原因是两者字段名存在不一样的
        picture.setUrl(uploadPictureResult.getUrl());
        //存的是缩略图的url
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        //存放的是空间的id
        picture.setSpaceId(spaceId);

        //支持外层传递图片名称
        String picName = uploadPictureResult.getName();
        if(pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        //给图片名设置默认值
        if (StrUtil.isEmpty(picName)) {
            //给图片名字默认设置
            picName = "img_" + UUID.randomUUID().toString().substring(0,8);
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId()); // ✅ 补充用户ID
        picture.setPicColor(uploadPictureResult.getPicColor());
        String format = String.format("这是一个 %s 图片", picName);
        picture.setIntroduction(format);
        //补充过审参数
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
//        //开启事务
//        Long finalSpaceId = spaceId;
        //牺牲部分业务，来使代码的舒服
        Long finalSpaceId = spaceId;
        //确保清除cos上的文件是旧图片
        Picture finalOldPicture = oldPicture;
        transactionTemplate.execute(status -> {
            //插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIF(!result, ErrorCode.PARAMS_ERROR, "图片上传失败");
            //todo 如果是更新（因为拼接上传路径有一个uuid，这个uuid是随机的），可以清理图片资源
            if (finalOldPicture != null) {
                this.clearPicture(finalOldPicture);
            }
            //如果finalSpaceId为空，则不上传到私有空间中
            if(ObjUtil.isNotEmpty(finalSpaceId) ) {
                //更新空间的使用额度，更新成功为true
                boolean update =spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIF(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            //随便返回一个，因为用不到
            return null;
        });
        //更新操作，用来删除存在cos上的服务，删除旧的图片
        return PictureVO.objToVo(picture);

    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = pictureQueryRequest.getId();
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
        Long spaceId = pictureQueryRequest.getSpaceId();
        Date endEditTime = pictureQueryRequest.getEndEditTIme();
        Date startEditTime = pictureQueryRequest.getStartEditTIme();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();

//        从多字段中搜索
        if (StrUtil.isNotEmpty(searchText)) {
//            需要拼接查询条件
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }

//        第一个参数是判断该id是否为空
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        /**
         * 这个是控制，是否查spaceId为空的时候呗
         * 在我们的查询逻辑中，如果为ture,则查询spaceId为null的情况
         * 如果为false的话，spaceId查询的条件就是 where spaceId =#{spaceId}
         *
         * 如果想查询公共图库使nullSpaceId =true
         * 查询私有空间 nullSpaceId == false
         */
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotEmpty(name), "name", name);
        queryWrapper.like(StrUtil.isNotEmpty(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotEmpty(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotEmpty(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus),"reviewStatus",reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewUserId), "reviewUserId", reviewUserId);
        //大于等于startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "endEditTIme", startEditTime);
        //小于endEditTime
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "endEditTIme", endEditTime);
        //以上来查询条件筛选出endEditTIme 在 [startEditTIme, endEditTIme) 区间内的数据
        //        JSON数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        //        排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), "asecend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
//        对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
//        关联查询用户信息
        Long userId = pictureVO.getUserId();
        if (userId != null || userId > 0) {
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
// //1.关联查询用户信息
//将所有图片的userId放进一个set中,避免重复查询
//set类型可以自动去重,List不行
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
        //通过userId查询所有的用户
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //2.填充信息
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
    public void deletePicture(DeleteRequest deleteRequest, HttpServletRequest request) {
        //1.判断传入的请求是否为空
        if (deleteRequest == null || deleteRequest.getId() <= 0){
            ThrowUtils.throwIF(true,ErrorCode.PARAMS_ERROR);
        }
//        获取id
        //获取当前用户状态
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        //2.判断数据库中的图片是否存在
        Picture picture = this.getById(id);
        ThrowUtils.throwIF(picture == null,ErrorCode.PARAMS_ERROR,"查询数据库失败");
        ///  改为使用Sa-Token（注解鉴权）
        //空间与公共图库的权限管理
        //checkPictureAuth(loginUser,picture);
//        if(!userService.isAdmin(loginUser) ||!picture.getUserId().equals(loginUser.getId()) ){
//            ThrowUtils.throwIF(true,ErrorCode.PARAMS_ERROR);
//        }

        Long finalSpaceId = picture.getSpaceId();
        //开启事务
        transactionTemplate.execute(status -> {
            //3.操作数据库
            //插入数据
            boolean result = this.removeById(id);
            //todo 如果是更新（因为拼接上传路径有一个uuid，这个uuid是随机的），可以清理图片资源
            this.clearPicture(picture);
            ThrowUtils.throwIF(!result, ErrorCode.PARAMS_ERROR,"删除图片失败");
            if (picture.getSpaceId() != null) {
                //更新空间的使用额度
                boolean update =spaceService.lambdaUpdate()
                        .eq(Space::getId,finalSpaceId)
                        .setSql("totalSize = totalSize - " + picture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIF(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            //用于清理cos空间
            this.clearPicture(picture);
            //随便返回一个，因为用不到
            return result;
        });
        //清理对象存储的图片存储
        this.clearPicture(picture);
        return ;
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
        if (id == null || reviewStatusEnum == null ) {
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
        updatePicture.setReviewStatus(reviewStatusEnum.getValue());

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
        ThrowUtils.throwIF(count >100,ErrorCode.PARAMS_ERROR, "最多抓取100条");
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
        }catch (Exception e) {
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
                fileUrl = fileUrl.substring(0, questMarkIndex) ;
            }
            //4.上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            pictureUploadRequest.setSpaceId(null);
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

    @Override
    public Picture editPicture(@RequestBody PictureEditRequest pictureEditRequest, User loginUser) {
        //数据库操作
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest,picture);
        //注意tags的转换
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        //设置编辑时间
        picture.setEditTime(new Date());
        //数据校验
        this.validPicture(picture);
        //判断是否存在
        Long id = pictureEditRequest.getId();
        //查询数据库
        Picture oldPicture = this.getById(id);
        ///  改为使用Sa-Token（注解鉴权）
        //仅本人或管理员可以编辑
        //this.checkPictureAuth(loginUser,oldPicture);
        //补充过审
        this.fillReviewParams(picture, loginUser);
        boolean result = this.updateById(picture);
        ThrowUtils.throwIF(!result,ErrorCode.SYSTEM_ERROR,"数据库操作失败");
        return picture;
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIF(spaceId == null, ErrorCode.PARAMS_ERROR, "空间id不能为空");
        ThrowUtils.throwIF(picColor == null, ErrorCode.PARAMS_ERROR, "颜色不能为空");
        ThrowUtils.throwIF(loginUser == null, ErrorCode.NO_AUTH_ERROR, "用户不能为空");
        //2.校验空间权限
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "不存在该空间");
        } else if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该用户没有这个空间的权限");
        }
        //3.查询该空间下的所有图片
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)  //1.查询的图片必须为该空间下的
                .isNotNull(Picture::getPicColor)    //2.查询的picture不为空
                .list();
        //如果没有图片直接返回空列表
        if (ObjUtil.isNull(pictureList)) {
            return new ArrayList<>();
        }
        //有图片的话。将颜色字符串转换为主色调
        Color targetColor = Color.decode(picColor);
        //4.计算相似度并排序
        List<Picture> sortPictureList = pictureList.stream()
                //默认是升序排序
                .sorted(Comparator.comparing(picture -> {
                    String hexColor = picture.getPicColor();
                    //没有主色调的图片默认排序到最后
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    //转换为color对象
                    Color pictureColor = Color.decode(hexColor);
                    //计算相似度
                    //因为方法返回的是1-相似度，而我们设置没有主色调数值最大，而流的排序是升序(从小-》大)，
                    //那么越小的值，相似度越大，给方法返回的添加负号，刚好符合逻辑
                    //比如返回的是(a0.9与b0.8),不添加负号时候, a排在b后面，反之，a才排在b前面，这样才对
                    return -ColorSimilarUtils.calculateSimilarity(pictureColor, targetColor);
                }))
                .limit(12)
                .collect(Collectors.toList());
        //5.返回结果
        return sortPictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    @Override
    public void editPitureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        //1.获取和校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        String spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();

        ThrowUtils.throwIF(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIF(spaceId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIF(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        //2.校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIF(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该用户没有权限操作此空间");
        }
        //3.查询指定图片(仅选择需要的字段)
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (ObjUtil.isNull(pictureList)) {
            return;
        }
        //4。更新分类和标签
        pictureList.forEach(picture -> {
            if (ObjUtil.isNotEmpty(category)) {
                picture.setCategory(category);
            }
            if (ObjUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        // 批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);
        //5.操作数据库进行查询
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIF(!result, ErrorCode.NOT_FOUND_ERROR, "批量编辑失败");
    }

    /**
     * alk扩图
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        //获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();

        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIF(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        ///  改为使用Sa-Token（注解鉴权）
        //检验授权
        //checkPictureAuth(loginUser, picture);
        //创建扩图任务
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();

        /**
         *
         */
        String imgerUrl = picture.getUrl() + "?x-oss-process=image/format,webp";
        input.setImageUrl( imgerUrl);
        log.info("url : {}" ,input.getImageUrl());
//        input.setImageUrl("https://www.gstatic.com/webp/gallery/1.jpg");
        createOutPaintingTaskRequest.setInput(input);
        createOutPaintingTaskRequest.setParameters(createPictureOutPaintingTaskRequest.getParameters());
        //调用任务
        return aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);
    }

    /**
     * 查询ai扩图的结果
     * @param taskId
     * @return
     */
    @Override
    public GetOutPaintingTaskResponse getPicturePaintingTask(String taskId) {
        return aliYunAiApi.getOutPaintingTask(taskId);
    }

    /**
     * nameRule  格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    public void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                // 如果模板里还没有 {序号}，就自动补上“_” + 字面量 {序号}
                if (!nameRule.contains("{序号}")) {
                    nameRule = StrUtil.format("{}_{{}}", nameRule, "序号"); // 输出：原串_{序号}
                }
                String template = nameRule.replace("{序号}", "{}");
                String picName  = StrUtil.format(template, count++);
                picture.setName(picName);
            }

        } catch (Exception e) {
            log.error("名称解析异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析异常");
        }
    }

    //实现异步
    @Async
    @Override
    public void clearPicture(Picture oldPicture) {
        //判断图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getId,pictureUrl)
                        .count();
        if (count > 1) {
            return;
        }
        //对象存储中的图片
        cosManager.deleteObject(pictureUrl);
        //删除对象存储中的搜缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if(StrUtil.isNotBlank(thumbnailUrl)){
            cosManager.deleteObject(thumbnailUrl);
        }
        //删除在数据库中的存储
//        if (oldPicture.getIsDelete() == 1) {
//            this.removeById(oldPicture.getId());
//        }

    }


    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId   = picture.getSpaceId();
        Long loginUserId = loginUser.getId();
        if(spaceId == null ){
            //此时操作的是公共图片
            if(!userService.isAdmin(loginUser) && !loginUserId.equals(picture.getUserId())){
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }else{
            //此时是私有空间
            //仅空间管理员有权限
            if (!loginUserId.equals(picture.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

    }

    public static void main(String[] args) {
        int count = 0;
        String nameRule = "图片";
        nameRule = StrUtil.format("{}_{{}}", nameRule, "序号"); // 输出：原串_{序号}
        String template = nameRule.replace("{序号}", "{}");
        String picName = StrUtil.format(template, count++);
        System.out.println(picName);
    }
}




