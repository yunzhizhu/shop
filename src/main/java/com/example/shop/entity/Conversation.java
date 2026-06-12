package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会话实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("conversation")
public class Conversation {

    /**
     * 会话ID
     */
    @TableId("conversation_id")
    private String conversationId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会话类型(1-私信,2-系统通知)
     */
    @TableField("conversation_type")
    private Integer conversationType;

    /**
     * 目标ID(私信=对方用户ID,系统通知=0)
     */
    @TableField("target_id")
    private Long targetId;

    /**
     * 目标名称
     */
    @TableField("target_name")
    private String targetName;

    /**
     * 目标头像
     */
    @TableField("target_avatar")
    private String targetAvatar;

    /**
     * 最后一条消息ID
     */
    @TableField("last_message_id")
    private Long lastMessageId;

    /**
     * 最后一条消息内容
     */
    @TableField("last_message_content")
    private String lastMessageContent;

    /**
     * 最后消息时间
     */
    @TableField("last_message_time")
    private LocalDateTime lastMessageTime;

    /**
     * 未读消息数
     */
    @TableField("unread_count")
    private Integer unreadCount;

    /**
     * 总消息数
     */
    @TableField("total_count")
    private Integer totalCount;

    /**
     * 是否置顶(0-否,1-是)
     */
    @TableField("is_pinned")
    private Integer isPinned;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}