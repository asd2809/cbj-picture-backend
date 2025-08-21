package com.yupi.cbjpicturebackend.model.dto.picture;


import com.yupi.cbjpicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PictureQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

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
     * 标签
     */
    private List<String> tags;
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
     *
     */
    private Double picScale;
    /**
     * 搜索词
     */
    private String searchText;
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 图片格式
     */
    private String picFormat;
    /**
     * 审核状态; 0-待审核，1-通过，2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人ID
     */
    private Long reviewUserId;
    /**
     * 空间id
     */

    private Long spaceId;

    /**
     * 这个是控制，是否查spaceId为空的时候呗
     * 在我们的查询逻辑中，如果为ture,则查询spaceIdw为null的情况
     * 如果为false的话，spaceId查询的条件就是 where spaceId =#{spaceId}
     */
    private boolean nullSpaceId;

    /**
     * 开始编辑时间
     */
    private Date startEditTIme;
    /**
     * 结束编辑时间
     */
    private Date endEditTIme;
    private static final long serialVersionUID = 1L;

}
