package com.example.shop.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息历史响应DTO
 */
@Data
public class MessageHistoryResponse {

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
     * 消息内容
     */
    private String content;

    /**
     * 消息类型
     */
    private Integer messageType;

    /**
     * 是否已读
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 图片URL(仅图片消息)
     */
    private String imageUrl;

    /**
     * 图片URL列表(仅图片消息)
     */
    private List<String> imageUrls;

    /**
     * 是否为系统消息
     */
    private Boolean isSystem;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 接收者ID（管理员查询时表示接收者数量）
     */
    private Long receiverId;

    /**
     * 接收者名称（管理员查询时为接收者列表，逗号分隔）
     */
    private String receiverName;

    /**
     * 接收者数量（仅管理员查询系统通知时使用）
     */
    private Integer receiverCount;

    /**
     * 内容类型
     */
    private Integer contentType;
}
