package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 好友请求实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("friend_request")
public class FriendRequest {

    /**
     * 请求ID
     */
    @TableId(value = "request_id", type = IdType.AUTO)
    private Long requestId;

    /**
     * 发送者ID
     */
    @TableField("from_user_id")
    private Long fromUserId;

    /**
     * 接收者ID
     */
    @TableField("to_user_id")
    private Long toUserId;

    /**
     * 验证消息
     */
    @TableField("message")
    private String message;

    /**
     * 状态(0-待处理,1-已接受,2-已拒绝,3-已过期)
     */
    @TableField("status")
    private Integer status;

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

    /**
     * 处理时间
     */
    @TableField("handled_at")
    private LocalDateTime handledAt;
}
