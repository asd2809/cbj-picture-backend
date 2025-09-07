package com.yupi.yupictureDDD.shared.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * 图片分表需要的算法
 */
//public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {
//
//    /**
//     * 第一个参数是所有支持的分表
//     * @param availableTargetNames
//     * @param preciseShardingValue
//     * @return
//     */
//    @Override
//    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {
//        // 编写分表逻辑，返回实际要查询的表名
//        Long spaceId = preciseShardingValue.getValue();
//        /// 逻辑表，没有分表之前的表
//        String logicTableName = preciseShardingValue.getLogicTableName();
//        // picture_0 物理表，picture 逻辑表
//        //如果spaceId为空，表示查公共图片
//        if (spaceId == null ) {
//            return logicTableName;
//        }
//        //根据spaceId 动态生成分表名
//        String realTableName ="picture_" + spaceId;
//        if(availableTargetNames.contains(realTableName)){
//            return realTableName;
//        }else{
//            return logicTableName;
//        }
//    }
//
//    @Override
//    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
//        return new ArrayList<>();
//    }
//
//    @Override
//    public Properties getProps() {
//        return null;
//    }
//
//    @Override
//    public void init(Properties properties) {
//
//    }
//}
public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {
        Long spaceId = preciseShardingValue.getValue();
        String logicTableName = preciseShardingValue.getLogicTableName();
        // spaceId 为 null 表示查询所有图片
        if (spaceId == null) {
            return logicTableName;
        }
        // 根据 spaceId 动态生成分表名
        String realTableName = "picture_" + spaceId;
        if (availableTargetNames.contains(realTableName)) {
            return realTableName;
        } else {
            return logicTableName;
        }
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return new ArrayList<>();
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }
}


