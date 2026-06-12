package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("message")
public class Message {

    /**
     * 消息ID
     */
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    /**
     * 会话ID
     */
    @TableField("conversation_id")
    private String conversationId;

    /**
     * 发送者ID(0表示系统消息)
     */
    @TableField("sender_id")
    private Long senderId;

    /**
     * 接收者ID
     */
    @TableField("receiver_id")
    private Long receiverId;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 消息类型(1-文本,2-图片)
     */
    @TableField("message_type")
    private Integer messageType;

    /**
     * 内容类型(1-普通私信,2-系统通知,3-订单通知,4-活动通知)
     */
    @TableField("content_type")
    private Integer contentType;

    /**
     * 图片URL
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 是否已读(0-未读,1-已读)
     */
    @TableField("is_read")
    private Integer isRead;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
