package com.yupi.yupictureDDD.shared.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

/**
 * websocket 配置（定义连接）
 */
@Configuration
@EnableWebSocket  /// 启用websocket支持
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private PictureEditHandler pictureEditHandler;
    @Resource
    private WsHandshakeInterceptor wsHandshakeInterceptor;

    /**
     * 用来注册websocket的处理器
     * @param registry
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pictureEditHandler, "/ws/picture/edit")  ///  注册一个websocket端点，pictureEditHandler是消息处理器
                .addInterceptors(wsHandshakeInterceptor)            /// 握手拦截器
                .setAllowedOrigins("*");                          /// 设置运行跨域来源
    }
}
