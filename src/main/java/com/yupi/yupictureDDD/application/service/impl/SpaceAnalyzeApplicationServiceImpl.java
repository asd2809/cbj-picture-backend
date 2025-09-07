package com.yupi.yupictureDDD.application.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupictureDDD.application.service.PictureApplicationService;
import com.yupi.yupictureDDD.application.service.SpaceApplicationService;
import com.yupi.yupictureDDD.infrastructure.exception.BusinessException;
import com.yupi.yupictureDDD.infrastructure.exception.ErrorCode;
import com.yupi.yupictureDDD.infrastructure.exception.ThrowUtils;
import com.yupi.yupictureDDD.domain.picture.entity.Picture;
import com.yupi.yupictureDDD.domain.space.entity.Space;
import com.yupi.yupictureDDD.domain.user.entity.User;
import com.yupi.yupictureDDD.application.service.SpaceAnalyzeApplicationService;
import com.yupi.yupictureDDD.interfaces.dto.space.analyze.*;
import com.yupi.yupictureDDD.interfaces.vo.space.anlyze.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpaceAnalyzeApplicationServiceImpl
        implements SpaceAnalyzeApplicationService {
    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private PictureApplicationService pictureApplicationService;

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIF(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 全空间或公共图库，从picture表中查询
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            //校验权限
            checkSpaceAnalyze(spaceUsageAnalyzeRequest, loginUser);
            //统计图库的使用空间
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            //补充查询条件
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
            List<Object> pictureList = pictureApplicationService.getBaseMapper().selectObjs(queryWrapper);
            //计算总大小
            long userSize = pictureList.stream().mapToLong(obj -> (Long) obj).sum();
            //计算使用的数量
            long userCount = pictureList.size();
            //封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(userSize);
            spaceUsageAnalyzeResponse.setUserCount(userCount);
            //公共图库和全部空间无数量和容量限制
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setUserUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {
            // 特定空间直接从space表中的一个字段查询就好了
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIF(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIF(space == null, ErrorCode.NOT_FOUND_ERROR);
            //校验权限,仅管理员有权限
            checkSpaceAnalyze(spaceUsageAnalyzeRequest, loginUser);
            long count = pictureApplicationService.lambdaQuery()
                    .eq(Picture::getSpaceId,space.getId()).count();
            //封装返回对象
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 /space.getMaxSize(),2 ).doubleValue();
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 /space.getMaxCount(),2 ).doubleValue();


            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setUserCount(count);
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            spaceUsageAnalyzeResponse.setUserUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    /**
     * 这个为什么实现类与空间资源使用分析不同，
     * 是因为，需求分析的时候，管理员并没有这个 空间分类分析的功能
     * @param spaceCategoryRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryRequest spaceCategoryRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIF(spaceCategoryRequest == null, ErrorCode.PARAMS_ERROR);
        // 全部图库
            checkSpaceAnalyze(spaceCategoryRequest, loginUser);
            ///多了分类查询
            //统计图库的分类图片
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            fillAnalyzeQueryWrapper(spaceCategoryRequest, queryWrapper);
            /// COUNT(*) AS count ,统计当前组内的数据数，并起别名count返回
            /// SUM(pic_size) AS total 统计当前组内所有pic_size的综合，并起别名total返回
            queryWrapper.select("category, COUNT(*) AS count, SUM(picSize) AS total");
            queryWrapper.groupBy("category")
                    .isNotNull("category");;
            //补充查询

            //目标是查询每个分类下的图片数量以及大小，
        List<Map<String, Object>> maps = pictureApplicationService.getBaseMapper().selectMaps(queryWrapper);
        return      maps
                    .stream()
                    .map(result ->{
                        String category = result.get("category").toString();
                        Number count = (Number) result.get("count");
                        Number total = (Number) result.get("total");
                        return new SpaceCategoryAnalyzeResponse(category,count.longValue(),total.longValue());
                    })
                    .collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTageAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIF(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        //校验全校
        checkSpaceAnalyze(spaceTagAnalyzeRequest, loginUser);
        //设置查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        queryWrapper.select("tags");
        //补充查询条件
        //查询数据库
        //提取每一个图片存放的tags字符串，并通过list的形式存放
        List<String> tagsList = pictureApplicationService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotEmpty)
                .map(Object::toString)
                .collect(Collectors.toList());
        //要统计每一个 标签出现的次数
        Map<String, Long> tagCountMap = tagsList.stream()
                /// 把每一条 JSON 数组字符串 → 转成 Java List
                ///
                /// 然后把所有 List 里的元素打平成一个大流
                ///
                /// 最终得到的是一个 包含所有标签的 Stream<String>
                .flatMap(tagsJson ->JSONUtil.toList(tagsJson,String.class).stream())
                .collect(Collectors.groupingBy(tag ->tag,Collectors.counting()));
        /// 转换为相应的对象

        return tagCountMap.entrySet().stream()
                .sorted((e1,e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(
                        entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue())
                ).collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIF(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        //权限
        checkSpaceAnalyze(spaceSizeAnalyzeRequest, loginUser);
        //构建查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("picSize");
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
        List<Long> picSizeList = pictureApplicationService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> (Long)size)
                .collect(Collectors.toList());
        //定义分段范围、注意使用有序的map
        Map<String ,Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizeList.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizeList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizeList.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizeList.stream().filter(size -> size >= 1 * 1024 * 1024).count());
        //转换为响应对象
        return sizeRanges.entrySet().stream()
                .map( result -> {
                    return new SpaceSizeAnalyzeResponse(result.getKey(), result.getValue());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIF(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyze(spaceUserAnalyzeRequest, loginUser);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

        //补充用户id查询
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq("userId", userId);
        //补充分析维度,每日，每周，每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                //按天统计记录
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                //按周统计记录
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                //按月统计记录
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }
        queryWrapper.groupBy("period");
        List<Map<String, Object>> results = pictureApplicationService.getBaseMapper().selectMaps(queryWrapper);
        //返回响应对象
        return results.stream()
                .map(result ->{
                    String period = result.get("period").toString();
                    Long count = (Long) result.get("count");
                    return new SpaceUserAnalyzeResponse(period, count);
                }).collect(Collectors.toList());

    }

    @Override
    public List<Space> getSpaceRankAnalyzes(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        // 参数
        ThrowUtils.throwIF(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 权限
        ThrowUtils.throwIF(!loginUser.isAdmin(), ErrorCode.PARAMS_ERROR);
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id","spaceName","userId","totalSize")
                .orderByDesc("totalSize")
                .last("limit " + spaceRankAnalyzeRequest.getToN());
        //查询结构并封装
        return spaceApplicationService.list(queryWrapper);
    }

    /**
     * 校验空间分析的权限，全部图库，私有空间，公共图库
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyze(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        //校验参数是否为空
        ThrowUtils.throwIF(spaceAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        //从数据库中获取
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        //全空间分析或者公共图库权限校验，仅管理员可以访问
        if (queryAll || queryPublic) {
            ThrowUtils.throwIF(!loginUser.isAdmin(), ErrorCode.PARAMS_ERROR);
        } else {
            //这个是对私有空间分析
            ThrowUtils.throwIF(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIF(space == null, ErrorCode.NOT_FOUND_ERROR);
        }
    }

    /**
     * 根据请求对象封装查询条件(填充空间分析需要的条件)
     *
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        ThrowUtils.throwIF(spaceAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        //1.全部空间
        if (queryAll) {
            return;
        }
        //2.公共图库
        if (queryPublic) {
            queryWrapper.isNull(String.valueOf(spaceId));
            return;
        }
        //3.私有空间
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "进行空间分析的时候，未指定查询范围");
    }

}
