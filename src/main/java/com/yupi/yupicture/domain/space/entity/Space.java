package com.yupi.yupicture.domain.space.entity;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import com.yupi.yupicture.domain.space.valueobject.SpaceLevelEnum;
import com.yupi.yupicture.domain.space.valueobject.SpaceTypeEnum;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.interfaces.dto.space.SpaceAddRequest;
import lombok.Data;

/**
 * 空间
 *
 * @TableName space
 */
@TableName(value = "space")
@Data
public class Space implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型；0-私有 1-团队
     */
    private Integer spaceType;
    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 添加关联用户的id
     */
    private static final long serialVersionUID = 1L;

    public void fill(SpaceAddRequest spaceAddRequest){
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            this.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            this.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (this.getSpaceType() == null) {
            this.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
    }
    public void validSpace( boolean add) {
        ThrowUtils.throwIF(this == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        //查询id判断id是否为空
        String spaceName = this.getSpaceName();
        Integer spaceLevel = this.getSpaceLevel();
        Integer spaceType = this.getSpaceType();
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
}