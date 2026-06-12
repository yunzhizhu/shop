package com.example.shop.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WebSocket消息传输DTO
 */
@Data
public class MessageDTO {

    /**
     * 消息ID
     */
    private Long messageId;

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
     * 消息类型
     */
    private Integer messageType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 图片URL列表(仅图片消息)
     */
    private List<String> imageUrls;

    /**
     * 是否为系统消息
     */
    private Boolean isSystem;
}
