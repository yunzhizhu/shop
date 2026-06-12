package com.example.shop.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息列表响应DTO
 */
@Data
public class MessageListResponse {

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
     * 预览内容
     */
    private String previewContent;

    /**
     * 未读消息数
     */
    private Integer unreadCount;

    /**
     * 最后消息时间
     */
    private LocalDateTime lastTime;

    /**
     * 是否为系统消息
     */
    private Boolean isSystem;

    /**
     * 消息类型
     */
    private Integer messageType;
}
