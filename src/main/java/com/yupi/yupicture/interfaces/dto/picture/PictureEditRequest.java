package com.yupi.yupicture.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureEditRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片简介
     */
    private String introduction;
    /**
     * 分类
     */
    private String category;

    /**
     * 标签(json)数组
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
