package com.yupi.cbjpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量导入图片请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {


    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取的数量
     */
    private Integer count = 30;

    /**
     * 图片前缀词
     */
    private String namePrefix;

    private static final long serialVersionDIU = 1L;
}
