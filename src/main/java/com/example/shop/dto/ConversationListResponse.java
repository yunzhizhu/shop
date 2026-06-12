package com.example.shop.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表响应DTO
 */
@Data
public class ConversationListResponse {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 会话类型(1-私信,2-系统通知)
     */
    private Integer conversationType;

    /**
     * 目标ID(私信=对方用户ID,系统通知=0)
     */
    private Long targetId;

    /**
     * 目标名称
     */
    private String targetName;

    /**
     * 目标头像
     */
    private String targetAvatar;

    /**
     * 最后一条消息内容
     */
    private String lastMessageContent;

    /**
     * 最后消息时间
     */
    private LocalDateTime lastMessageTime;

    /**
     * 未读消息数
     */
    private Integer unreadCount;

    /**
     * 总消息数
     */
    private Integer totalCount;

    /**
     * 是否置顶(0-否,1-是)
     */
    private Integer isPinned;

    /**
     * 最后一条消息类型(1-文本,2-图片)
     */
    private Integer lastMessageType;

    /**
     * 最后一条消息的内容类型(1-普通私信,2-系统通知,3-订单通知,4-活动通知)
     */
    private Integer lastContentType;
}