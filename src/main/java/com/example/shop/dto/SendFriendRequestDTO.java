package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 发送好友请求DTO
 */
@Data
public class SendFriendRequestDTO {

    /**
     * 接收者ID
     */
    @NotNull(message = "接收者ID不能为空")
    private Long toUserId;

    /**
     * 验证消息
     */
    private String message;
}
