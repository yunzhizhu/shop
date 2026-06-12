package com.example.shop.controller;

import com.example.shop.common.Result;
import com.example.shop.dto.UnreadUpdateDTO;
import com.example.shop.dto.WebSocketMessageDTO;
import com.example.shop.websocket.NativeWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 调试控制器
 * 使用原生 WebSocket
 */
@Slf4j
@RestController
@RequestMapping("/websocket-debug")
public class WebSocketDebugController {

    /**
     * 查看当前 WebSocket 连接状态
     */
    @GetMapping("/connections")
    @PreAuthorize("permitAll()")
    public Result getConnections() {
        int userCount = NativeWebSocketHandler.getOnlineUserCount();
        log.info("当前 WebSocket 连接数: {}", userCount);
        
        return Result.success(Map.of(
            "onlineUserCount", userCount,
            "message", "当前在线用户数: " + userCount
        ));
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/check-online/{userId}")
    @PreAuthorize("permitAll()")
    public Result checkOnline(@PathVariable Long userId) {
        boolean isOnline = NativeWebSocketHandler.isUserOnline(userId);
        log.info("检查用户在线状态: userId={}, isOnline={}", userId, isOnline);
        
        return Result.success(Map.of(
            "userId", userId,
            "isOnline", isOnline
        ));
    }

    /**
     * 手动推送测试消息
     */
    @PostMapping("/push-test/{userId}")
    @PreAuthorize("permitAll()")
    public Result pushTest(@PathVariable Long userId, @RequestParam String content) {
        try {
            WebSocketMessageDTO messageDTO = new WebSocketMessageDTO();
            messageDTO.setMessageId(9999L);
            messageDTO.setSenderId(1L);
            messageDTO.setSenderName("测试");
            messageDTO.setContent(content);
            messageDTO.setMessageType(1);
            messageDTO.setContentType(1);
            
            Map<String, Object> message = new HashMap<>();
            message.put("type", "message");
            message.put("targetUserId", userId);
            message.put("data", messageDTO);
            
            log.info("手动推送测试消息: targetUserId={}, content={}", userId, content);
            
            NativeWebSocketHandler.sendToUser(userId, message);
            
            return Result.success("推送成功, targetUserId=" + userId);
        } catch (Exception e) {
            log.error("推送失败", e);
            return Result.error("推送失败: " + e.getMessage());
        }
    }

    /**
     * 手动推送未读数更新
     */
    @PostMapping("/push-unread/{userId}")
    @PreAuthorize("permitAll()")
    public Result pushUnread(@PathVariable Long userId) {
        try {
            UnreadUpdateDTO unreadUpdateDTO = new UnreadUpdateDTO("test_conversation", 99, 99);
            
            Map<String, Object> message = new HashMap<>();
            message.put("type", "unread");
            message.put("targetUserId", userId);
            message.put("data", unreadUpdateDTO);
            
            log.info("手动推送未读数更新: targetUserId={}", userId);
            
            NativeWebSocketHandler.sendToUser(userId, message);
            
            return Result.success("推送成功, targetUserId=" + userId);
        } catch (Exception e) {
            log.error("推送失败", e);
            return Result.error("推送失败: " + e.getMessage());
        }
    }

    /**
     * 广播测试消息
     */
    @PostMapping("/broadcast")
    @PreAuthorize("permitAll()")
    public Result broadcast(@RequestParam String content) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "broadcast");
            message.put("content", content);
            message.put("timestamp", System.currentTimeMillis());
            
            log.info("广播测试消息: content={}", content);
            
            NativeWebSocketHandler.broadcast(message);
            
            return Result.success("广播成功");
        } catch (Exception e) {
            log.error("广播失败", e);
            return Result.error("广播失败: " + e.getMessage());
        }
    }
}
