package com.yupi.cbjpicturebackend.manager.websocket;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.yupi.cbjpicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.yupi.cbjpicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.yupi.cbjpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.yupi.cbjpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.yupi.cbjpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.yupi.cbjpicturebackend.model.entity.User;
import com.yupi.cbjpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 图片编辑WebSocket 处理器
 */
@Slf4j
@Component
public class PictureEditHandler extends TextWebSocketHandler {
    /// 每张图片的编辑状态，key :pictureId, value:正在编辑的用户id
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();
    /// 保存所有连接会话,key:pictureId ,value:用户会话集合
    /// WebSocketSession是Websocket连接的会话对象
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private PictureEditEventProducer pictureEditEventProducer;

    /**
     * 连接了客户端之后
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 保存会话到集合中
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);
        // 构造响应，发送加入编辑的消息
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
        responseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 加入编辑", user.getUserName());
        ///  用于发送给前端
        responseMessage.setMessage(message);
        responseMessage.setUserVO(userService.getUserVO(user));

        //广播给所有用户
        broadcastPictureEditing(pictureId, responseMessage);

    }

    /**
     * 收到前端发送的消息，根据消息类别处理消息
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        // 获取消息内容,解析为PictureEditMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        /// 从session中获取到公共参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");

        //根据消息类型处理消息（生产消息到Disruptor 环形队列中）
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage,session,user,pictureId);

        /// 根据消息类型处理消息
//        switch (enumByValue) {
//            case ENTER_EDIT:
//                handleEnterEditMessage(pictureEditResponseMessage, session, user, pictureId);
//                break;
//            case EDIT_ACTION:
//                handleEditActionMessage(pictureEditResponseMessage, session, user, pictureId);
//                break;
//            case EXIT_EDIT:
//                handleExitEditMessage(pictureEditResponseMessage, session, user, pictureId);
//                break;
//            default:
//                break;
//        }
    }

    /**
     * 关闭客户端
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        // 从Session属性中获取到公共参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        /// 移除当前用户的编辑状态
        handleExitEditMessage(null, session, user, pictureId);
        /// 删除会话
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (webSocketSessions != null) {
            webSocketSessions.remove(session);
            if (webSocketSessions.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
        responseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 离开编辑", user.getUserName());
        responseMessage.setMessage(message);
        responseMessage.setUserVO(userService.getUserVO(user));
        /// 广播给所有用户
        broadcastPictureEditing(pictureId, responseMessage);
    }

    /**
     * 进入编辑状态
     *
     * @param pictureEditResponseMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditResponseMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        /// 没有用户进行编辑该图片，才能进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 设置用户正在编辑该图片
            pictureEditingUsers.put(pictureId, user.getId());
            // 构造响应
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("用户 %s 进入编辑", user.getUserName());
            responseMessage.setMessage(message);
            responseMessage.setUserVO(userService.getUserVO(user));
            /// 广播给所有用户
            broadcastPictureEditing(pictureId, responseMessage);
        }

    }

    /**
     * 处理编辑操作
     *
     * @param pictureEditResponseMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditResponseMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        /// 正在编辑的用户
        Long editUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditResponseMessage.getEditAction();
        PictureEditActionEnum pictureEditActionEnum = PictureEditActionEnum.getEnumByValue(editAction);

        /// 判断是否为有效编辑
        if (pictureEditActionEnum == null) {
            return;
        }
        /// 确认是不是当前的编辑者
        if (editUserId != null && editUserId.equals(user.getId())) {
            // 构造响应
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("用户 %s 执行 %s", user.getUserName(), pictureEditActionEnum.getText());
            responseMessage.setMessage(message);
            responseMessage.setEditAction(editAction);
            responseMessage.setUserVO(userService.getUserVO(user));
            /// 广播给所有用户(除了当前客户端之外的其他用户，否则会造成重复编辑)
            broadcastPictureEditing(pictureId, responseMessage, session);
        }
    }

    /**
     * 退出编辑操作
     *
     * @param pictureEditResponseMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditResponseMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        /// 正在编辑的用户
        Long editUserId = pictureEditingUsers.get(pictureId);
        /// 确认是当前的编辑者
        if (editUserId != null && editUserId.equals(user.getId())) {
            pictureEditingUsers.remove(pictureId);
            // 构造响应
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("用户 %s 退出编辑", user.getUserName());
            responseMessage.setMessage(message);
            responseMessage.setUserVO(userService.getUserVO(user));
            /// 广播给所有用户
            broadcastPictureEditing(pictureId, responseMessage);
        }
    }

    /**
     * 广播给编辑图片的所有用户(支持排除自己)
     *
     * @param pictureId
     * @param pictureEditRequestMessage
     */
    private void broadcastPictureEditing(Long pictureId, PictureEditResponseMessage pictureEditRequestMessage, WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(webSocketSessions)) {
            /**
             * 解决Long类型发送给前端，精度丢失的问题
             */
            //创建ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            /// 配置序列化，将Long转换为String,解决丢失精度问题
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance); /// 支持long基本类型
            objectMapper.registerModule(module);
            /// 序列化为字符串
            String message = objectMapper.writeValueAsString(pictureEditRequestMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession webSocketSession : webSocketSessions) {
                ///  排除的session不发送
                if (excludeSession != null && excludeSession.equals(webSocketSession)) {
                    continue;
                }
                if (webSocketSession.isOpen()) {
                    webSocketSession.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 广播给编辑该图片的所有用户
     *
     * @param pictureId
     * @param pictureEditRequestMessage
     * @throws IOException
     */
    private void broadcastPictureEditing(Long pictureId, PictureEditResponseMessage pictureEditRequestMessage) throws IOException {
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(webSocketSessions)) {
            /**
             * 解决Long类型发送给前端，精度丢失的问题
             */
            //创建ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            /// 配置序列化，将Long转换为String,解决丢失精度问题
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance); /// 支持long基本类型
            objectMapper.registerModule(module);
            /// 序列化为字符串
            String message = objectMapper.writeValueAsString(pictureEditRequestMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession webSocketSession : webSocketSessions) {
                if (webSocketSession.isOpen()) {
                    /// 如果不适应disruptor,这里可能会阻塞
                    webSocketSession.sendMessage(textMessage);
                }
            }
        }
    }

}
