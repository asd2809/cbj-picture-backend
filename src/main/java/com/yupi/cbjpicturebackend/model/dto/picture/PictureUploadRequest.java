package com.yupi.cbjpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

//图片上传请求
@Data
public class PictureUploadRequest implements Serializable {
    /**
     * 图片id()
     */
    private Long id;

    /**
     *
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    private static final long serialVersionUID = 1L;

}
