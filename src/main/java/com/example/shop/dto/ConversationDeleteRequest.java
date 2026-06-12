package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 删除会话请求DTO
 */
@Data
public class ConversationDeleteRequest {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String conversationId;
}