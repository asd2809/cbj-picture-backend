package com.yupi.yupicture.domain.picture.entity;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import lombok.Data;

/**
 * 图片
 *
 * @TableName picture
 */
@TableName(value = "picture")
@Data
public class Picture {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 图片主色调
     */
    private String picColor;
    /**
     * 审核状态: 0-待审核;1-通过;2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;
    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图url
     */
    private String thumbnailUrl;
    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private String tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 创建用户 id
     */
    private Long userId;
    /**
     *空间id
     */
    private Long spaceId;
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

    public void validPicture() {

        //      从对象中取值
        Long id = this.getId();
        String url = this.getUrl();
        String introduction = this.getIntroduction();
//        修改数据,id不能为空,有参数校验
        ThrowUtils.throwIF(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id不能为空");
        if(StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIF(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        if(StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIF(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }
}