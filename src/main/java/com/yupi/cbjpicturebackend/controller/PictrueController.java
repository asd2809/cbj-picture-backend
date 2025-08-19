package com.yupi.cbjpicturebackend.controller;


import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.cbjpicturebackend.annotation.AuthCheck;
import com.yupi.cbjpicturebackend.common.BaseResponse;
import com.yupi.cbjpicturebackend.common.DeleteRequest;
import com.yupi.cbjpicturebackend.common.ResultUtils;
import com.yupi.cbjpicturebackend.constant.UserConstant;
import com.yupi.cbjpicturebackend.exception.ErrorCode;
import com.yupi.cbjpicturebackend.exception.ThrowUtils;
import com.yupi.cbjpicturebackend.model.dto.picture.*;
import com.yupi.cbjpicturebackend.model.entity.Picture;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.model.enums.PictureReviewStatusEnum;
import com.yupi.cbjpicturebackend.model.vo.PictureTagCategory;
import com.yupi.cbjpicturebackend.model.vo.PictureVO;
import com.yupi.cbjpicturebackend.service.PictureService;
import com.yupi.cbjpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictrueController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
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
    public BaseResponse<Boolean> deletePicutre(DeleteRequest deleteRequest,HttpServletRequest request){
//        服务类的代码应该写在这里的
        BaseResponse<Boolean> booleanBaseResponse = pictureService.deletePicture(deleteRequest, request);
        return booleanBaseResponse;
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
//      1.判断传入的请求是否为空
        ThrowUtils.throwIF(pictureUpdateRequest==null || pictureUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR,"web请求的参数错误");

//      2.操作数据库
//      把传入的请求对象转换为picture
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest,picture);
//        注意将list转为string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
//        数据校验
        pictureService.validPicture(picture);
//        先把请求的id通过数据库查询,是否存在这个图片
        Picture oldPicture = pictureService.getById(pictureUpdateRequest.getId());
        ThrowUtils.throwIF(oldPicture == null,ErrorCode.PARAMS_ERROR,"该图片不存在数据库中");
//       补充过审
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        boolean result = pictureService.updateById(picture);
//       这个才是真正的进行更新操作
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
//        普通用户默认只能看到审核通过的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //       操作数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        ThrowUtils.throwIF(picturePage==null,ErrorCode.SYSTEM_ERROR,"数据库操作失败");
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage,request));
    }
    /**
     * 分页查询(封装类)
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request){

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
    public BaseResponse<PictureVO> getPictureVOById(Long id){
        //1.判断请求是否为空
        ThrowUtils.throwIF(id==null,ErrorCode.PARAMS_ERROR,"传入图片的id为空");
        //2.操作数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIF(picture==null,ErrorCode.SYSTEM_ERROR,"查不到这个图片");
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
//        判断请求是否为空
        ThrowUtils.throwIF(pictureEditRequest==null || pictureEditRequest.getId() <=0,ErrorCode.PARAMS_ERROR,"web传入的参数错误");
//      数据库操作
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest,picture);
//        注意tags的转换
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
//      设置编辑时间
        picture.setEditTime(new Date());
//      数据校验
        pictureService.validPicture(picture);
        User loginUser = userService.getLoginUser(request);
//       判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(id);
//      仅本人或管理员可以编辑
        if(!oldPicture.getUserId().equals(loginUser.getId()) || !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())){
            ThrowUtils.throwIF(true,ErrorCode.NO_AUTH_ERROR,"编辑图片既不是本人也不是管理员");
        }
//      补充过审
        pictureService.fillReviewParams(picture, loginUser);
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIF(!result,ErrorCode.SYSTEM_ERROR,"数据库操作失败");

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

}
