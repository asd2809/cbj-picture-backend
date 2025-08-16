package com.yupi.cbjpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

//图片上传请求
@Data
public class PictureAddRequest implements Serializable {
    /**
     * 图片id()
     */
    private String id;

    private static final long serialVersionUID = 1L;

}
