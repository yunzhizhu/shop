package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 标记会话已读请求DTO
 */
@Data
public class ConversationReadRequest {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String conversationId;
}