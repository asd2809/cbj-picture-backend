package com.yupi.yupictureDDD.application.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupictureDDD.application.service.UserApplicationService;
import com.yupi.yupictureDDD.domain.space.service.SpaceDomainService;
import com.yupi.yupictureDDD.infrastructure.exception.BusinessException;
import com.yupi.yupictureDDD.infrastructure.exception.ErrorCode;
import com.yupi.yupictureDDD.infrastructure.exception.ThrowUtils;
import com.yupi.yupictureDDD.interfaces.dto.space.SpaceAddRequest;
import com.yupi.yupictureDDD.interfaces.dto.space.SpaceQueryRequest;
import com.yupi.yupictureDDD.domain.space.entity.Space;
import com.yupi.yupictureDDD.domain.space.entity.SpaceUser;
import com.yupi.yupictureDDD.domain.user.entity.User;
import com.yupi.yupictureDDD.domain.space.valueobject.SpaceLevelEnum;
import com.yupi.yupictureDDD.domain.space.valueobject.SpaceRoleEnum;
import com.yupi.yupictureDDD.domain.space.valueobject.SpaceTypeEnum;
import com.yupi.yupictureDDD.interfaces.vo.space.SpaceVO;
import com.yupi.yupictureDDD.interfaces.vo.user.UserVO;
import com.yupi.yupictureDDD.application.service.SpaceApplicationService;
import com.yupi.yupictureDDD.application.service.SpaceUserApplicationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import com.yupi.yupictureDDD.infrastructure.mapper.SpaceMapper;
/**
 * @author leneve
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-08-20 15:49:06
 */
@Service
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceApplicationService {

    @Resource
    private UserApplicationService userApplicationService;


    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;
    /**
     * 与事务有关
     *
     */
    @Resource
    private TransactionTemplate transactionTemplate;
    @Autowired
    private SpaceDomainService spaceDomainService;

//    @Resource
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIF(space == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        //查询id判断id是否为空
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getSpaceLevelEnum(spaceLevel);
        //如果是创建空间
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevelEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不存在这个空间级别");
            }
            if (ObjectUtil.isEmpty(spaceType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不确定空间类型");
            }
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
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不存在");
        }
    }




    @Override
    public SpaceVO getSpaceVO(Space Space) {
        ThrowUtils.throwIF(Space == null, ErrorCode.PARAMS_ERROR, "传入的空间参数为空");
        SpaceVO spaceVO = SpaceVO.objToVo(Space);
        //关联查询user
        Long userId = spaceVO.getUserId();
        if (userId != null || userId > 0) {
            //从数据库中查询
            User user = userApplicationService.getUserById(userId);
            UserVO userVO = userApplicationService.getUserVO(user);
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
        Map<Long, List<User>> useIdUserListMap = userApplicationService.listByIds(userIdSet)
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
            spaceVO.setUserVO(userApplicationService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        spaceDomainService.fillSpaceBySpaceLevel(space);
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
        space.fill(spaceAddRequest);
        //注意可能前端会传空
        BeanUtils.copyProperties(spaceAddRequest, space);
        //填充容量和大小
        this.fillSpaceBySpaceLevel(space);
        //2.校验参数
        space.validSpace(true);
        //3.校验权限，非管理员只能创建普通的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !loginUser.isAdmin()){
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
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        /// 补一个查询条件，可以用来判断是否为团队空间
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
                if (exists) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "一种类型的空间每个用户只能创建一个");
                }
                //存入数据库
                boolean result = this.save(space);
                //创建私有空间
                ThrowUtils.throwIF(!result, ErrorCode.PARAMS_ERROR, "保存空间到数据库失败");
                if (space.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
                    //创建成员记录(给创建人设置)
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    result = spaceUserApplicationService.save(spaceUser);
                    ThrowUtils.throwIF(!result, ErrorCode.PARAMS_ERROR, "创建空间创建者失败");

                }
//                /// 仅对团队空间生效
//                // 创建分表
//                dynamicShardingManager.createSpacePictureTable(space);
                //返回新写入的数据id
                return space.getId();
            });
            return newSpaceId;
        }
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        spaceDomainService.checkSpaceAuth(loginUser, space);
    }
}




