package com.example.shop.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 原生 WebSocket 处理器
 * 不使用 STOMP 协议，直接使用 JSON 消息
 */
@Slf4j
@Component
public class NativeWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    public NativeWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        // 注册 Java 8 日期时间模块
        this.objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳的功能
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // 存储所有活跃的 WebSocket 连接
    // 键: userId，值: WebSocketSession
    private static final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存储 session 到 userId 的映射
    // 键: sessionId，值: userId
    private static final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        log.info("[WebSocket] 新连接建立: sessionId={}", sessionId);

        // 从 session 属性中获取 userId（在握手拦截器中设置）
        Long userId = (Long) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        if (userId != null) {
            // 保存连接
            sessions.put(userId, session);
            sessionUserMap.put(sessionId, userId);

            log.info("[WebSocket] 用户连接成功: userId={}, username={}, sessionId={}", userId, username, sessionId);
            log.info("[WebSocket] 当前在线用户数: {}", sessions.size());

            // 发送连接成功消息
            sendMessage(session, Map.of(
                    "type", "connected",
                    "message", "WebSocket 连接成功",
                    "userId", userId
            ));
        } else {
            log.warn("[WebSocket] 连接缺少用户信息，关闭连接: sessionId={}", sessionId);
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        Long userId = sessionUserMap.get(sessionId);

        if (userId == null) {
            log.warn("[WebSocket] 收到未认证连接的消息: sessionId={}", sessionId);
            return;
        }

        String payload = message.getPayload();
        log.info("[WebSocket] 收到消息: userId={}, payload={}", userId, payload);

        try {
            // 解析消息
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
            String type = (String) msg.get("type");

            switch (type) {
                case "ping":
                    // 心跳请求
                    sendMessage(session, Map.of(
                            "type", "pong",
                            "timestamp", System.currentTimeMillis()
                    ));
                    break;

                case "send":
                    // 发送消息（这里可以调用 MessageService 处理）
                    log.info("[WebSocket] 用户发送消息: userId={}, data={}", userId, msg.get("data"));
                    // 待实现：调用 MessageService.sendMessage()
                    break;

                default:
                    log.warn("[WebSocket] 未知消息类型: type={}", type);
                    break;
            }
        } catch (Exception e) {
            log.error("[WebSocket] 处理消息失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        Long userId = sessionUserMap.get(sessionId);

        if (userId != null) {
            sessions.remove(userId);
            sessionUserMap.remove(sessionId);
            log.info("[WebSocket] 连接关闭: userId={}, sessionId={}, status={}", userId, sessionId, status);
            log.info("[WebSocket] 当前在线用户数: {}", sessions.size());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        Long userId = sessionUserMap.get(sessionId);
        log.error("[WebSocket] 传输错误: userId={}, sessionId={}, error={}", userId, sessionId, exception.getMessage(), exception);

        if (session.isOpen()) {
            session.close();
        }
    }

    /**
     * 发送消息到指定用户
     */
    public static void sendToUser(Long userId, Map<String, Object> message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            sendMessage(session, message);
            log.info("[WebSocket] 消息已发送: userId={}, type={}", userId, message.get("type"));
        } else {
            log.warn("[WebSocket] 用户不在线或连接已关闭: userId={}", userId);
        }
    }

    /**
     * 广播消息到所有在线用户
     */
    public static void broadcast(Map<String, Object> message) {
        int count = 0;
        for (Map.Entry<Long, WebSocketSession> entry : sessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (session.isOpen()) {
                sendMessage(session, message);
                count++;
            }
        }
        log.info("[WebSocket] 广播消息完成: type={}, 发送数={}", message.get("type"), count);
    }

    /**
     * 发送消息到 session
     */
    private static void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // 注册 Java 8 日期时间模块
            mapper.registerModule(new JavaTimeModule());
            // 禁用将日期写为时间戳的功能
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            
            String json = mapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("[WebSocket] 发送消息失败: sessionId={}, error={}", session.getId(), e.getMessage(), e);
        }
    }

    /**
     * 获取在线用户数
     */
    public static int getOnlineUserCount() {
        return sessions.size();
    }

    /**
     * 检查用户是否在线
     */
    public static boolean isUserOnline(Long userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }
}
