package com.yupi.yupictureDDD.shared.websocket.model;


import com.yupi.yupictureDDD.interfaces.vo.user.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片编辑响应类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditResponseMessage {
    /**
     * 消息类型
     */
    private String type;
    /**
     * 信息
     */
    private String message;
    /**
     * 执行的编辑动作
     */
    private String editAction;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 用户信息（兼容前端使用）
     */
    private UserVO userVO;

    // 设置user字段时同时设置userVO字段
    public void setUser(UserVO user) {
        this.user = user;
        this.userVO = user;
    }
}
