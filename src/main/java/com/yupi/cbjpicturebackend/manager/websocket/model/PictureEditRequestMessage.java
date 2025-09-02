package com.yupi.cbjpicturebackend.manager.websocket.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 图片编辑请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditRequestMessage {


    /**
     * 消息类型
     */
    private String type;
    /**
     * 执行的编辑动作
     */
    private String editAction;
}
