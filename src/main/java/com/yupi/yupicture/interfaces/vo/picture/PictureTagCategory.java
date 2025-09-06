package com.yupi.yupicture.interfaces.vo.picture;

import lombok.Data;

import java.util.List;

/**
 *
 * 图片标签分类列表,
 * 主要是为了页面初始化的标签与分类
 */
@Data
public class PictureTagCategory {

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 分类列表
     */
    private List<String> categoryList;
}
