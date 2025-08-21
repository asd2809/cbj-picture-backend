package com.yupi.cbjpicturebackend.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.cbjpicturebackend.exception.BusinessException;
import com.yupi.cbjpicturebackend.exception.ErrorCode;
import com.yupi.cbjpicturebackend.exception.ThrowUtils;
import com.yupi.cbjpicturebackend.model.dto.space.SpaceAddRequest;
import com.yupi.cbjpicturebackend.model.dto.space.SpaceQueryRequest;
import com.yupi.cbjpicturebackend.model.entity.Space;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.model.enums.SpaceLevelEnum;
import com.yupi.cbjpicturebackend.model.vo.SpaceVO;
import com.yupi.cbjpicturebackend.model.vo.UserVO;
import com.yupi.cbjpicturebackend.service.SpaceService;
import com.yupi.cbjpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import com.yupi.cbjpicturebackend.mapper.SpaceMapper;
/**
 * @author leneve
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-08-20 15:49:06
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;


    /**
     * 与事务有关
     *
     */
    @Resource
    private TransactionTemplate transactionTemplate;

    public SpaceServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIF(space == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        //查询id判断id是否为空
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getSpaceLevelEnum(spaceLevel);
        //如果是创建空间
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevelEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不存在这个空间级别");
            }
            return;
        }
        /**
         * -----修改空间的数据校验--------
         * 但是感觉没什么区别，索性就不写了
         *
         */
        if (StrUtil.isBlank(spaceName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
        }
        if (spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不存在这个空间级别");
        }
    }

    @Override
    public List<SpaceVO> getSpaceVOList(List<Space> spaceList) {
        //先判断是否为空
        if (ObjectUtil.isEmpty(spaceList)) {
            return new ArrayList<>();
        }

        return spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
    }

    @Override
    public SpaceVO getSpaceVO(Space Space, HttpServletRequest request) {
        ThrowUtils.throwIF(Space == null, ErrorCode.PARAMS_ERROR, "传入的空间参数为空");
        SpaceVO spaceVO = SpaceVO.objToVo(Space);
        //关联查询user
        Long userId = spaceVO.getUserId();
        if (userId != null || userId > 0) {
            //从数据库中查询
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUserVO(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();

        /**
         * 1.创建分页对象
         */

        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (ObjectUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        /**
         * 2.把分页对象转换为SpaceVO
         */
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        /**
         * 3.关联查询用户信息，把space中的userId填入进去
         */
        Set<Long> userIdSet = spaceList.stream()
                .map(Space::getUserId)
                .collect(Collectors.toSet());
        /**
         * 4.通过userId查询数据库中的userId
         */
        Map<Long, List<User>> useIdUserListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));
        /**
         * 5.填充信息
         */
        spaceVOList.forEach(spaceVO -> {
            //获取该空间的userId
            Long userId = spaceVO.getUserId();
            User user = null;
            //为了避免查不到这个用户
            if (useIdUserListMap.containsKey(userId)) {
                user = useIdUserListMap.get(userId).get(0);
            }
            //填充用户
            spaceVO.setUserVO(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIF(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR, "空间查询请求为空");
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Long userId = spaceQueryRequest.getUserId();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq(ObjectUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(ObjectUtil.isNotEmpty(spaceName), "spaceName", spaceName);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), "asecend".equals(sortOrder), sortField);

        return queryWrapper;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        //传入的时候检验空间级别是否存在
        ThrowUtils.throwIF(space.getSpaceLevel() == null, ErrorCode.PARAMS_ERROR, "需要提供空间级别");
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getSpaceLevelEnum(space.getSpaceLevel());
        if (spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        long maxCount = spaceLevelEnum.getMaxCount();
        long maxSize = spaceLevelEnum.getMaxSize();
        /**
         * 判断管理员是否自己设置了最大图片数量以及最大大小
         */
        if(space.getMaxCount() == null){
            space.setMaxCount(maxCount);
        }
        if(space.getMaxSize() == null){
            space.setMaxSize(maxSize);
        }
    }

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        //1.填充参数默认值
        Space space = new Space();
        //注意可能前端会传空
        BeanUtils.copyProperties(spaceAddRequest, space);
        //填充容量和大小
        this.fillSpaceBySpaceLevel(space);
        //2.校验参数
        validSpace(space,true);
        //3.校验权限，非管理员只能创建普通的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限创建指定级别的空间");
        }
        //4.控制一个用户只能创建一个私有空间
        //intern()的作用是把字符串放入字符串常量池中国，并返回池中唯一的引用，

        String lock = String.valueOf(userId).intern();
        //使用用户的id的字符串对象作为锁
        //保证同一个用户id的操作不会并发执行，不同用户拿到的是不同的锁，因此互不影响，可以并发创建
        synchronized (lock) {
            /**
             *添加了事务，可以保证查与插要么全部成功，要么全部失败
             */
            Long newSpaceId = transactionTemplate.execute(status ->  {
                //判断是否已有空间
                boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                if (exists) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "每个用户只能创建一个私有空间");
                }
                //存入数据库
                boolean result = this.save(space);
                //创建私有空间
                ThrowUtils.throwIF(!result, ErrorCode.PARAMS_ERROR, "保存空间到数据库失败");
                //返回新写入的数据id
                return space.getId();
            });
            return newSpaceId;
        }
    }
}




