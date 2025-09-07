package com.yupi.yupictureDDD.infrastructure.manager.upload.file;

import lombok.Data;

//上传图片的结果
@Data
public class UploadPictureResult {
    /**
     * 图片 url
     */
    private String url;
    /**
     * 图片主色调
     */
    private String picColor;
    /**
     * 缩略图的url
     */
    private String thumbnailUrl;
    /**
     * 图片名称
     */
    private String name;

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




}
