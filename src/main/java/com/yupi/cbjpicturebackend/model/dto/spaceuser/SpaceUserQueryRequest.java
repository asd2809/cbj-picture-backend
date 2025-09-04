package com.yupi.cbjpicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserQueryRequest implements Serializable {

    /**
     *
     */
    private Long id;
    /**
     *
     */
    private Long spaceId;

    /**
     * 用户Id
     */
    private Long userId;

    /**
     * 空间角色/viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;

}
