package com.yupi.yupicture.interfaces.vo.space;


import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.interfaces.vo.user.UserVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

@Data
public class SpaceUserVo implements Serializable {

    /**
     *
     */
    private Long userId;
    /**
     *
     */
    private Long spaceId;
    /**
     *
     */
    private String spaceRole;

    /**
     *
     */
    private SpaceVO spaceVO;

    /**
     * 原始Space对象，用于兼容前端代码
     */
    private Space space;

    private UserVO userVO;

    private Date createTime;
    private Date updateTime;
    private Date editTime;

    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象
     */

    public static SpaceUser voToObj(SpaceUserVo spaceUserVo) {
        if (spaceUserVo == null) {
            return null;
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserVo, spaceUser);
        return spaceUser;
    }

    /**
     * 对象转封装类
     */
    public static SpaceUserVo objToVo(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;

        }
        SpaceUserVo spaceUserVo = new SpaceUserVo();
        BeanUtils.copyProperties(spaceUser, spaceUserVo);
        return spaceUserVo;
    }
}
