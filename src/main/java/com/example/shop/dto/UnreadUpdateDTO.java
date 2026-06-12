package com.example.shop.dto;

import lombok.Data;

/**
 * 未读数更新推送DTO
 */
@Data
public class UnreadUpdateDTO {

    /**
     * 推送类型：unread
     */
    private String pushType = "unread";

    /**
     * 目标用户ID（用于前端过滤）
     */
    private Long targetUserId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 该会话的未读数
     */
    private Integer unreadCount;

    /**
     * 总未读数
     */
    private Integer totalUnreadCount;

    public UnreadUpdateDTO(String conversationId, Integer unreadCount, Integer totalUnreadCount) {
        this.conversationId = conversationId;
        this.unreadCount = unreadCount;
        this.totalUnreadCount = totalUnreadCount;
    }
}