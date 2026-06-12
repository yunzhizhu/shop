package com.example.shop.websocket;

import com.example.shop.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 原生 WebSocket 握手拦截器
 * 用于认证和设置用户信息
 */
@Slf4j
@Component
public class NativeWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        log.info("[WebSocket] 握手开始: URI={}", request.getURI());

        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            // 从查询参数中获取 token
            String token = servletRequest.getParameter("token");
            if (token == null || token.isEmpty()) {
                log.warn("[WebSocket] 握手失败: Token 为空");
                return false;
            }

            log.info("[WebSocket] 提取到 token: {}...", token.substring(0, Math.min(20, token.length())));

            try {
                // 从 token 中提取用户信息
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);

                // 验证 token（使用提取出的 username）
                if (!jwtUtil.validateToken(token, username)) {
                    log.warn("[WebSocket] 握手失败: Token 无效");
                    return false;
                }

                // 将用户信息存储到 WebSocket session 属性中
                attributes.put("userId", userId);
                attributes.put("username", username);
                attributes.put("token", token);

                log.info("[WebSocket] 握手认证成功: userId={}, username={}", userId, username);
                return true;

            } catch (Exception e) {
                log.error("[WebSocket] 握手失败: Token 解析异常", e);
                return false;
            }
        }

        log.warn("[WebSocket] 握手失败: 请求类型不支持");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("[WebSocket] 握手后异常", exception);
        }
    }
}
