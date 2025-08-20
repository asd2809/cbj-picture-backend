package com.yupi.cbjpicturebackend.model.dto.space;


import lombok.Data;

import java.io.Serializable;

/**
 * 用户编辑空间的请求
 */
@Data
public class SpaceEditRequest implements Serializable {
    /**
     * 空间的id
     */
    private Long id;
    /**
     * 空间名称
     */
    private String spaceName;

    private static final long serialVersionUID = 1L;


}
