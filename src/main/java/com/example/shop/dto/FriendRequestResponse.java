package com.example.shop.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友请求响应DTO
 */
@Data
public class FriendRequestResponse {

    /**
     * 请求ID
     */
    private Long requestId;

    /**
     * 发送者ID
     */
    private Long fromUserId;

    /**
     * 发送者用户名
     */
    private String fromUsername;

    /**
     * 发送者头像
     */
    private String fromAvatar;

    /**
     * 接收者ID
     */
    private Long toUserId;

    /**
     * 接收者用户名
     */
    private String toUsername;

    /**
     * 接收者头像
     */
    private String toAvatar;

    /**
     * 验证消息
     */
    private String message;

    /**
     * 状态(0-待处理,1-已接受,2-已拒绝,3-已过期)
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 处理时间
     */
    private LocalDateTime handledAt;
}
