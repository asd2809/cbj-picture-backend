package com.yupi.cbjpicturebackend.model.vo;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.yupi.cbjpicturebackend.model.entity.Picture;
import com.yupi.cbjpicturebackend.model.entity.Space;
import com.yupi.cbjpicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class SpaceVO implements Serializable {

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
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 空间级别:0-私有 1-团队
     */
    private Integer spaceRole;
    /**
     * 创建用户
     */
    private UserVO userVO;
    /**
     * 添加权限列表
     */
    private List<String> permissions = new ArrayList<>();

    private static final long serialVersionUID = 1L;


    /**
     * 封装类转对象
     */

    public static Space voToObj(SpaceVO pictureVO) {
        if (pictureVO == null) {
            return null;
        }
        Space space = new Space();
        BeanUtils.copyProperties(pictureVO, space);
        return space;
    }

    /**
     * 对象转封装类
     */
    public static SpaceVO objToVo(Space space) {
        if (space == null) {
            return null;

        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space, spaceVO);
        return spaceVO;
    }
}
