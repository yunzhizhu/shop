package com.example.shop.exception;

/**
 * 会话访问权限不足异常
 */
public class ConversationAccessDeniedException extends BusinessException {

    public ConversationAccessDeniedException() {
        super(3005, "无权操作此会话");
    }

    public ConversationAccessDeniedException(String message) {
        super(3005, message);
    }

    public ConversationAccessDeniedException(String conversationId, Long userId) {
        super(3005, String.format("无权操作此会话: conversationId=%s, userId=%d", conversationId, userId));
    }
}