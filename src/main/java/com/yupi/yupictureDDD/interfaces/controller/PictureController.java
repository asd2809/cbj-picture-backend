package com.yupi.yupictureDDD.interfaces.controller;


import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.yupictureDDD.application.service.PictureApplicationService;
import com.yupi.yupictureDDD.application.service.SpaceApplicationService;
import com.yupi.yupictureDDD.application.service.UserApplicationService;
import com.yupi.yupictureDDD.infrastructure.annotation.AuthCheck;
import com.yupi.yupictureDDD.infrastructure.api.aliyunai.AliYunAiApi;
import com.yupi.yupictureDDD.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupictureDDD.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.yupictureDDD.infrastructure.common.BaseResponse;
import com.yupi.yupictureDDD.infrastructure.common.DeleteRequest;
import com.yupi.yupictureDDD.infrastructure.common.ResultUtils;
import com.yupi.yupictureDDD.domain.user.constant.UserConstant;
import com.yupi.yupictureDDD.infrastructure.exception.ErrorCode;
import com.yupi.yupictureDDD.infrastructure.exception.ThrowUtils;
import com.yupi.yupictureDDD.shared.auth.SpaceUserAuthManager;
import com.yupi.yupictureDDD.shared.auth.StpKit;
import com.yupi.yupictureDDD.shared.auth.annotation.SaSpaceCheckPermission;
import com.yupi.yupictureDDD.shared.auth.model.SpaceUserPermissionConstant;
import com.yupi.yupictureDDD.domain.picture.entity.Picture;
import com.yupi.yupictureDDD.domain.space.entity.Space;
import com.yupi.yupictureDDD.domain.user.entity.User;
import com.yupi.yupictureDDD.domain.picture.valueobject.PictureReviewStatusEnum;
import com.yupi.yupictureDDD.interfaces.Assembler.PictureAssembler;
import com.yupi.yupictureDDD.interfaces.dto.picture.*;
import com.yupi.yupictureDDD.interfaces.vo.picture.PictureTagCategory;
import com.yupi.yupictureDDD.interfaces.vo.picture.PictureVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private PictureApplicationService pictureApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;
    @Resource
    private SpaceApplicationService spaceApplicationService;
    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE =Caffeine.newBuilder()
                                    .initialCapacity(1024)
                                    .maximumSize(10000L)
                                    // 缓存 5 分钟移除
                                    .expireAfterWrite(Duration.ofMillis(5))
                                    .build();
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@RequestParam("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request
                                          ) {
        User LoginUser = userApplicationService.getLoginUser(request);
        PictureVO pictureVO = pictureApplicationService.uploadPicture(multipartFile,pictureUploadRequest,LoginUser);
        return ResultUtils.success(pictureVO);
    }
    /**
     * 通过Url上传图片（可重新上传）
     *
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                      HttpServletRequest request){
        User loginUser = userApplicationService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureApplicationService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }
    /**
     * 根据id删除图片
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,HttpServletRequest request){
        //服务类的代码应该写在这里的
        pictureApplicationService.deletePicture(deleteRequest, request);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(PictureUpdateRequest pictureUpdateRequest,
                                               HttpServletRequest request) {
        //1.判断传入的请求是否为空
        ThrowUtils.throwIF(pictureUpdateRequest==null || pictureUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR,"web请求的参数错误");

        //2.操作数据库
        //把传入的请求对象转换为picture
        Picture picture = PictureAssembler.toPictureEntity(pictureUpdateRequest);
        //注意将list转为string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        //数据校验
        pictureApplicationService.validPicture(picture);
        //先把请求的id通过数据库查询,是否存在这个图片
        Picture oldPicture = pictureApplicationService.getById(pictureUpdateRequest.getId());
        ThrowUtils.throwIF(oldPicture == null,ErrorCode.PARAMS_ERROR,"该图片不存在数据库中");
        //补充过审
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.fillReviewParams(picture, loginUser);
        //这个才是真正的进行更新操作
        boolean result = pictureApplicationService.updateById(picture);

        ThrowUtils.throwIF(!result,ErrorCode.SYSTEM_ERROR,"数据库操作失败");
        return ResultUtils.success(true);
    }

    /**
     * 分页查询图片(仅限管理员))
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest){
//       先判断请求是否为空
        ThrowUtils.throwIF(pictureQueryRequest==null,
                ErrorCode.PARAMS_ERROR,"请求参数错误");
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        //       操作数据库
        //进行分页查询
        Page<Picture> picturePage = pictureApplicationService.page(new Page<>(current, pageSize),
                pictureApplicationService.getQueryWrapper(pictureQueryRequest));
        ThrowUtils.throwIF(picturePage==null,ErrorCode.SYSTEM_ERROR,"数据库操作失败");
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页查询(封装类)
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request){
        //先判断请求是否为空
        ThrowUtils.throwIF(pictureQueryRequest == null,
                ErrorCode.PARAMS_ERROR,"请求参数错误");
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIF(pageSize > 100,ErrorCode.PARAMS_ERROR,"最多只能同时查询100条图片");
        //空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            //普通用户默认只能看到审核通过的数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            //为了查询的时候使spaceId为null
            pictureQueryRequest.setNullSpaceId(true);
        }else{
            /// ture表示有权限，false表示没有权限
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIF(!hasPermission,ErrorCode.NO_AUTH_ERROR,"用户没权限");
            /// 改用Sa-Token编程式权限
            //            //查询私有空间
            //User loginUser = userApplicationService.getLoginUser(request);
            //Space space = spaceApplicationService.getById(spaceId);
            //ThrowUtils.throwIF(space == null,ErrorCode.SYSTEM_ERROR,"私有空间不存在");
            //if (!space.getUserId().equals(loginUser.getId())) {
            //throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间权限");
            //}
            //  //这个其实可以不写，因为只有NullSpaceId为true的时候，才会使查询的时候spaceId为null的条件
            //pictureQueryRequest.setNullSpaceId(false);
        }
        //       操作数据库
        Page<Picture> picturePage = pictureApplicationService.page(new Page<>(current, pageSize),
                pictureApplicationService.getQueryWrapper(pictureQueryRequest));

        return ResultUtils.success(pictureApplicationService.getPictureVOPage(picturePage,request));
    }
    /**
     * 分页查询(封装类)(带有缓存)
     * @param pictureQueryRequest
     * @return
     */
    //因为开始添加了私有空间，就没必要再有缓存处理了，因为用户的私有空间没必要缓存
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                                      HttpServletRequest request) {

        //先判断请求是否为空
        ThrowUtils.throwIF(pictureQueryRequest==null ,
                ErrorCode.PARAMS_ERROR,"请求参数错误");
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIF(pageSize > 20,ErrorCode.PARAMS_ERROR,"最多只能查20页");
//        普通用户默认只能看到审核通过的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //查询缓存。缓存中没有再查询数据库
        //构建缓存的key
        //1.把对象转换为json
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5Hex(queryCondition);
        String cacheKey = String.format("yupicture:listPictureVOByPage:%s",hashKey);

        //1.查询本地缓存(Caffeine)
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if(cachedValue != null){
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        //2.查redis缓存
        //操作redis
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String cachedPage = opsForValue.get(cacheKey);
        //3.如果redis缓存存在
        if (cachedPage != null) {
            //存入本地缓存
            LOCAL_CACHE.put(cacheKey,cachedPage);
            //如果缓存存在，获取结果
            Page<PictureVO> cachePage = JSONUtil.toBean(cachedPage, Page.class);
            return ResultUtils.success(cachePage);
        }
        //4.       操作数据库
        Page<Picture> picturePage = pictureApplicationService.page(new Page<>(current, pageSize),
                pictureApplicationService.getQueryWrapper(pictureQueryRequest));
        ThrowUtils.throwIF(picturePage==null,ErrorCode.SYSTEM_ERROR,"数据库操作失败");
        Page<PictureVO> pictureVOPage = pictureApplicationService.getPictureVOPage(picturePage, request);
        //5.更新缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        //6.存入本地缓存
        LOCAL_CACHE.put(cacheKey,cacheValue);
        //7.设置过期时间5-10分钟,注意缓存雪崩
        opsForValue.set(cacheKey, cacheValue,5, TimeUnit.MINUTES);

        return ResultUtils.success(pictureVOPage);
    }


    /**
     * 管理员通过id获取图片
     * @param id
     * @return
     */
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(Long id){
        //1.判断请求是否为空
        ThrowUtils.throwIF(id==null,ErrorCode.PARAMS_ERROR,"传入图片的id为空");
        //2.操作数据库
        Picture picture = pictureApplicationService.getById(id);
        ThrowUtils.throwIF(picture==null,ErrorCode.SYSTEM_ERROR,"查不到这个图片");
        return ResultUtils.success(picture);
    }

    /**
     * 根据id查询图片(封装类)
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id,HttpServletRequest request){
        //1.判断请求是否为空
        ThrowUtils.throwIF(id==null,ErrorCode.PARAMS_ERROR,"传入图片的id为空");
        //2.操作数据库
        Picture picture = pictureApplicationService.getById(id);
        ThrowUtils.throwIF(picture==null,ErrorCode.SYSTEM_ERROR,"查不到这个图片");
        User loginUser = userApplicationService.getLoginUser(request);
        //先判断该图片是否有spaceId，即这个图片是不是某个用户的私有
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null){
            /// 使用编程式注解
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIF(!hasPermission,ErrorCode.SYSTEM_ERROR);
            //公共图库的图片是随便获取的,但是该图片存储的私有空间，可能不是该用户的
            ///  改为使用Sa-Token（注解鉴权）
//            pictureApplicationService.checkPictureAuth(loginUser,picture);
            space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIF(space==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        }

        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureApplicationService.getPictureVO(picture,request);
        pictureVO.setPermissionList(permissionList);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 编辑图片(主要是用户使用)
     * @param pictureEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Picture> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request){
        //判断请求是否为空
        ThrowUtils.throwIF(pictureEditRequest == null, ErrorCode.PARAMS_ERROR, "web传入的参数错误");
        User loginUser = userApplicationService.getLoginUser(request);
        /// 在此处将实体类与DTO进行转换
        Picture picture1 = PictureAssembler.toPictureEntity(pictureEditRequest);
        Picture picture = pictureApplicationService.editPicture(picture1,loginUser);
        return ResultUtils.success(picture);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory(){
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门","感谢","生活","高清","艺术","校园","背景","简历","创意");
        List<String> cagetoryList = Arrays.asList("模板","电商","表情包","素材","海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(cagetoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureUpload(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIF(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
//        获取当前用户
        User loginUser = userApplicationService.getLoginUser(request);

        pictureApplicationService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量上传图片
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIF(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR,"传入的参数错误");
        User user = userApplicationService.getLoginUser(request);
        int uploadCount = pictureApplicationService.uploadPictureByBatch(pictureUploadByBatchRequest, user);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 以图搜图
     *
     * @param searcbPictureByColorRequest
     * @param request
     * @return
     */
//    @PostMapping("/search/picture")
//    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest){
//        ThrowUtils.throwIF(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
//        Long pictureId = searchPictureByPictureRequest.getPictureId();
//        ThrowUtils.throwIF(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
//        Picture oldPicture = pictureApplicationService.getById(pictureId);
//        ThrowUtils.throwIF(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(oldPicture.getUrl());
//        return ResultUtils.success(resultList);
//    }

    /**
     * 按照颜色搜索
     * @param searcbPictureByColorRequest
     * @param request
     * @return
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearcbPictureByColorRequest searcbPictureByColorRequest,
                                                              HttpServletRequest request) {
        ThrowUtils.throwIF(searcbPictureByColorRequest == null, ErrorCode.PARAMS_ERROR, "请求为空");
        User loginUser = userApplicationService.getLoginUser(request);
        String picColor = searcbPictureByColorRequest.getPicColor();
        Long spaceId = searcbPictureByColorRequest.getSpaceId();
        List<PictureVO> pictureVOList= pictureApplicationService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVOList);
    }

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIF(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR, "请求参数错误");
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.editPitureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 向第三方服务进行ai扩图请求(第一步)
     * @param createPictureOutPaintingTaskRequest
     * @param request
     * @return
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(@RequestBody
                                                                                        CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
                                     HttpServletRequest request){
        //校验参数
        ThrowUtils.throwIF(createPictureOutPaintingTaskRequest == null,
                ErrorCode.PARAMS_ERROR, "请求参数为空");
        User loginUser = userApplicationService.getLoginUser(request);
        CreateOutPaintingTaskResponse result =
                pictureApplicationService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     *查询ai扩图
     * @param taskId
     * @return
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTaskResponse(String taskId){
        ThrowUtils.throwIF(taskId == null, ErrorCode.PARAMS_ERROR, "查询ai扩图服务的taskId不能为空");
        GetOutPaintingTaskResponse result = pictureApplicationService.getPicturePaintingTask(taskId);
        return ResultUtils.success(result);
    }
//    /**
//     * 创建 AI 扩图任务
//     */
//    @PostMapping("/out_painting/create_task")
//    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
//            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
//            HttpServletRequest request) {
//        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userApplicationService.getLoginUser(request);
//        CreateOutPaintingTaskResponse response = pictureApplicationService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
//        return ResultUtils.success(response);
//    }
//
//    /**
//     * 查询 AI 扩图任务
//     */
//    @GetMapping("/out_painting/get_task")
//    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
//        ThrowUtils.throwIF(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
//        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
//        return ResultUtils.success(task);
//    }

}
