package com.example.shop.config;

import com.example.shop.websocket.NativeWebSocketHandler;
import com.example.shop.websocket.NativeWebSocketHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 * 使用原生 WebSocket，不使用 STOMP
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private NativeWebSocketHandler nativeWebSocketHandler;

    @Autowired
    private NativeWebSocketHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(nativeWebSocketHandler, "/ws-native")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
