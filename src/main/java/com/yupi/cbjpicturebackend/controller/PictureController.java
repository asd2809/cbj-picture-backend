package com.yupi.cbjpicturebackend.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.cbjpicturebackend.annotation.AuthCheck;
import com.yupi.cbjpicturebackend.api.aliyunai.AliYunAiApi;
import com.yupi.cbjpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.cbjpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.cbjpicturebackend.common.BaseResponse;
import com.yupi.cbjpicturebackend.common.DeleteRequest;
import com.yupi.cbjpicturebackend.common.ResultUtils;
import com.yupi.cbjpicturebackend.constant.UserConstant;
import com.yupi.cbjpicturebackend.exception.BusinessException;
import com.yupi.cbjpicturebackend.exception.ErrorCode;
import com.yupi.cbjpicturebackend.exception.ThrowUtils;
import com.yupi.cbjpicturebackend.model.dto.picture.*;
import com.yupi.cbjpicturebackend.model.entity.Picture;
import com.yupi.cbjpicturebackend.model.entity.Space;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.model.enums.PictureReviewStatusEnum;
import com.yupi.cbjpicturebackend.model.vo.PictureTagCategory;
import com.yupi.cbjpicturebackend.model.vo.PictureVO;
import com.yupi.cbjpicturebackend.service.PictureService;
import com.yupi.cbjpicturebackend.service.SpaceService;
import com.yupi.cbjpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
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
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;
    @Resource
    private SpaceService spaceService;
    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE =Caffeine.newBuilder()
                                    .initialCapacity(1024)
                                    .maximumSize(10000L)
                                    // 缓存 5 分钟移除
                                    .expireAfterWrite(Duration.ofMillis(5))
                                    .build();


    /**
     * 上传图片
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestParam("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request
                                          ) {
        User LoginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile,pictureUploadRequest,LoginUser);
        return ResultUtils.success(pictureVO);
    }
    /**
     * 通过Url上传图片（可重新上传）
     *
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                      HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }
    /**
     * 根据id删除图片
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(DeleteRequest deleteRequest,HttpServletRequest request){
        //服务类的代码应该写在这里的
        pictureService.deletePicture(deleteRequest, request);
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
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest,picture);
        //注意将list转为string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        //数据校验
        pictureService.validPicture(picture);
        //先把请求的id通过数据库查询,是否存在这个图片
        Picture oldPicture = pictureService.getById(pictureUpdateRequest.getId());
        ThrowUtils.throwIF(oldPicture == null,ErrorCode.PARAMS_ERROR,"该图片不存在数据库中");
        //补充过审
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        //这个才是真正的进行更新操作
        boolean result = pictureService.updateById(picture);

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
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
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
        ThrowUtils.throwIF(pageSize > 20,ErrorCode.PARAMS_ERROR);
        //空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            //普通用户默认只能看到审核通过的数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            //为了查询的时候使spaceId为null
            pictureQueryRequest.setNullSpaceId(true);
        }else{
            //查询私有空间
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIF(space == null,ErrorCode.SYSTEM_ERROR,"私有空间不存在");
            if (!space.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间权限");
            }
            //这个其实可以不写，因为只有NullSpaceId为true的时候，才会使查询的时候spaceId为null的条件
            pictureQueryRequest.setNullSpaceId(false);

        }
        //       操作数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        List<Picture> pictureList = picturePage.getRecords();
//        ThrowUtils.throwIF(picturePage.getRecords().isEmpty(),ErrorCode.SYSTEM_ERROR,"数据库操作失败");
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage,request));
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
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        ThrowUtils.throwIF(picturePage==null,ErrorCode.SYSTEM_ERROR,"数据库操作失败");
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
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
        Picture picture = pictureService.getById(id);
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
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIF(picture==null,ErrorCode.SYSTEM_ERROR,"查不到这个图片");
        User loginUser = userService.getLoginUser(request);
        //先判断该图片是否有spaceId，即这个图片是不是某个用户的私有
        Long spaceId = picture.getSpaceId();
        if (spaceId != null){
            //公共图库的图片是随便获取的
            //但是该图片存储的私有空间，可能不是该用户的
            pictureService.checkPictureAuth(loginUser,picture);
        }
        return ResultUtils.success(PictureVO.objToVo(picture));
    }

    /**
     * 编辑图片(主要是用户使用)
     * @param pictureEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Picture> editPicture(PictureEditRequest pictureEditRequest,HttpServletRequest request){
        //判断请求是否为空
        ThrowUtils.throwIF(pictureEditRequest == null, ErrorCode.PARAMS_ERROR, "web传入的参数错误");
        User loginUser = userService.getLoginUser(request);
        Picture picture = pictureService.editPicture(pictureEditRequest,loginUser);
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
        User loginUser = userService.getLoginUser(request);

        pictureService.doPictureReview(pictureReviewRequest, loginUser);
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
        User user = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, user);
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
//        Picture oldPicture = pictureService.getById(pictureId);
//        ThrowUtils.throwIF(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(oldPicture.getUrl());
//        return ResultUtils.success(resultList);
//    }
    @PostMapping("/search/color")
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearcbPictureByColorRequest searcbPictureByColorRequest,
                                                              HttpServletRequest request) {
        ThrowUtils.throwIF(searcbPictureByColorRequest == null, ErrorCode.PARAMS_ERROR, "请求为空");
        User loginUser = userService.getLoginUser(request);
        String picColor = searcbPictureByColorRequest.getPicColor();
        Long spaceId = searcbPictureByColorRequest.getSpaceId();
        List<PictureVO> pictureVOList= pictureService.searchPictureByColor(spaceId, picColor, loginUser);
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
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIF(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR, "请求参数错误");
        User loginUser = userService.getLoginUser(request);
        pictureService.editPitureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 向第三方服务进行ai扩图请求(第一步)
     * @param createPictureOutPaintingTaskRequest
     * @param request
     * @return
     */
    @PostMapping("/out_painting/create_task")
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(@RequestBody
                                                                                        CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
                                     HttpServletRequest request){
        //校验参数
        ThrowUtils.throwIF(createPictureOutPaintingTaskRequest == null,
                ErrorCode.PARAMS_ERROR, "请求参数为空");
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse result =
                pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(result);
    }
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTaskResponse(String taskId){
        ThrowUtils.throwIF(taskId == null, ErrorCode.PARAMS_ERROR, "查询ai扩图服务的taskId不能为空");
        GetOutPaintingTaskResponse result = pictureService.getPicturePaintingTask(taskId);
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
//        User loginUser = userService.getLoginUser(request);
//        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
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
