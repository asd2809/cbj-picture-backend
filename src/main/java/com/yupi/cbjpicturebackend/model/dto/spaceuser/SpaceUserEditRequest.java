package com.yupi.cbjpicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserEditRequest implements Serializable {

    /**
     *
     */
    private Long spaceUserId;

    /**
     * 空间角色/viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
