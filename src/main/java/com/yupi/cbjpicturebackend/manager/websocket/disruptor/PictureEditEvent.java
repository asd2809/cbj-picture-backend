package com.yupi.cbjpicturebackend.manager.websocket.disruptor;


import com.yupi.cbjpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.yupi.cbjpicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 定义事件
 * 图片编辑事件
 * 事件类
 * 充当上下文容器
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的session
     */
    private WebSocketSession session;
    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片id
     */
    private Long pictureId;
}
