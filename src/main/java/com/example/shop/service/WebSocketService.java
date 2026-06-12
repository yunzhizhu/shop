package com.example.shop.service;

import com.example.shop.dto.UnreadUpdateDTO;
import com.example.shop.dto.WebSocketMessageDTO;

/**
 * WebSocket服务接口
 */
public interface WebSocketService {

    /**
     * 推送新消息给用户
     *
     * @param userId 用户ID
     * @param messageDTO 消息DTO
     */
    void pushMessage(Long userId, WebSocketMessageDTO messageDTO);

    /**
     * 推送未读数更新给用户
     *
     * @param userId 用户ID
     * @param unreadUpdateDTO 未读数更新DTO
     */
    void pushUnreadUpdate(Long userId, UnreadUpdateDTO unreadUpdateDTO);

    /**
     * 推送好友请求通知给用户
     *
     * @param userId 用户ID
     */
    void pushFriendRequestNotification(Long userId);

    /**
     * 广播系统通知给所有在线用户
     *
     * @param messageDTO 系统通知消息DTO
     */
    void broadcastSystemNotification(WebSocketMessageDTO messageDTO);
}