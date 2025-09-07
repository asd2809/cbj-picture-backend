package com.yupi.yupictureDDD.interfaces.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;


/**
 * 通用的控件分析模板
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {


    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

    /**
     * 全空间分析
     */
    private boolean queryAll;

    private static final long serialVersionUID = 1L;
}
