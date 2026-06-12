package com.example.shop.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * WebSocket消息推送DTO
 */
@Data
public class WebSocketMessageDTO {

    /**
     * 推送类型：message-新消息，unread-未读数更新
     */
    private String pushType;

    /**
     * 目标用户ID（用于前端过滤）
     */
    private Long targetUserId;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 接收者ID
     */
    private Long receiverId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：1-文本，2-图片
     */
    private Integer messageType;

    /**
     * 内容类型：1-普通私信，2-系统通知，3-订单通知，4-活动通知
     */
    private Integer contentType;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 是否为系统消息
     */
    private Boolean isSystem;
}