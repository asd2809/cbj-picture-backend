package com.yupi.yupictureDDD.shared.auth.model;


import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用于接收配置文件的值
 */
@Data
public class SpaceUserAuthConfig implements Serializable {
    /**
     * 权限列表
     */
    private List<SpaceUserPermission> permissions;

    /**
     * 角色列表
     */

    private List<SpaceUserRole> roles;

    private static final long serialVersionUID = 1L;
}
