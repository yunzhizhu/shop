package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 会话置顶请求DTO
 */
@Data
public class ConversationPinRequest {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String conversationId;

    /**
     * 是否置顶(true-置顶,false-取消置顶)
     */
    @NotNull(message = "置顶状态不能为空")
    private Boolean isPinned;
}