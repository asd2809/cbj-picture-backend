package com.yupi.cbjpicturebackend.manager.websocket.model;


import com.yupi.cbjpicturebackend.model.vo.UserVO;
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
    private UserVO userVO;
}
