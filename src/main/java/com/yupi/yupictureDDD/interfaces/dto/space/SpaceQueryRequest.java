package com.yupi.yupictureDDD.interfaces.dto.space;

import com.yupi.yupictureDDD.infrastructure.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 查询空间的请求
 */
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;


    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 空间级别;0-私有 1-团队
     */
    private Integer spaceType;

    private static final long serialVersionUID = 1L;

}
