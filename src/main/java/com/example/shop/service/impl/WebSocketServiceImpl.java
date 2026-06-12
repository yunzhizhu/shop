package com.example.shop.service.impl;

import com.example.shop.dto.UnreadUpdateDTO;
import com.example.shop.dto.WebSocketMessageDTO;
import com.example.shop.service.WebSocketService;
import com.example.shop.websocket.NativeWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 服务实现类
 * 使用原生 WebSocket
 */
@Slf4j
@Service
public class WebSocketServiceImpl implements WebSocketService {

    @Override
    public void pushMessage(Long userId, WebSocketMessageDTO messageDTO) {
        try {
            log.info("[WebSocket] 推送消息: userId={}, messageId={}", userId, messageDTO.getMessageId());

            Map<String, Object> message = new HashMap<>();
            message.put("type", "message");
            message.put("targetUserId", userId);
            message.put("data", messageDTO);

            NativeWebSocketHandler.sendToUser(userId, message);

            log.info("[WebSocket] 消息推送成功: userId={}, messageId={}", userId, messageDTO.getMessageId());
        } catch (Exception e) {
            log.error("[WebSocket] 消息推送失败: userId={}, messageId={}", userId, messageDTO.getMessageId(), e);
        }
    }

    @Override
    public void pushUnreadUpdate(Long userId, UnreadUpdateDTO unreadUpdateDTO) {
        try {
            log.info("[WebSocket] 推送未读数更新: userId={}, conversationId={}, unreadCount={}",
                    userId, unreadUpdateDTO.getConversationId(), unreadUpdateDTO.getUnreadCount());

            Map<String, Object> message = new HashMap<>();
            message.put("type", "unread");
            message.put("targetUserId", userId);
            message.put("data", unreadUpdateDTO);

            NativeWebSocketHandler.sendToUser(userId, message);

            log.info("[WebSocket] 未读数更新推送成功: userId={}", userId);
        } catch (Exception e) {
            log.error("[WebSocket] 未读数更新推送失败: userId={}", userId, e);
        }
    }

    @Override
    public void pushFriendRequestNotification(Long userId) {
        try {
            log.info("[WebSocket] 推送好友请求通知: userId={}", userId);

            Map<String, Object> message = new HashMap<>();
            message.put("type", "friendRequest");
            message.put("targetUserId", userId);
            message.put("data", Map.of("message", "您有新的好友请求"));

            NativeWebSocketHandler.sendToUser(userId, message);

            log.info("[WebSocket] 好友请求通知推送成功: userId={}", userId);
        } catch (Exception e) {
            log.error("[WebSocket] 好友请求通知推送失败: userId={}", userId, e);
        }
    }

    @Override
    public void broadcastSystemNotification(WebSocketMessageDTO messageDTO) {
        try {
            log.info("[WebSocket] 广播系统通知: messageId={}", messageDTO.getMessageId());

            Map<String, Object> message = new HashMap<>();
            message.put("type", "message");
            message.put("data", messageDTO);

            NativeWebSocketHandler.broadcast(message);

            log.info("[WebSocket] 系统通知广播成功");
        } catch (Exception e) {
            log.error("[WebSocket] 系统通知广播失败", e);
        }
    }
}
