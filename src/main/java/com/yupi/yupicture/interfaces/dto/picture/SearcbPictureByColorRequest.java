package com.yupi.yupicture.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 按照颜色搜索图片
 */
@Data
public class SearcbPictureByColorRequest implements Serializable {

    /**
     * 颜色主色调
     */
    private String picColor;

    /**
     * 空间id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
