package com.example.shop.exception;

/**
 * 消息不存在异常
 */
public class MessageNotFoundException extends BusinessException {

    public MessageNotFoundException() {
        super(3004, "会话不存在");
    }

    public MessageNotFoundException(String message) {
        super(3004, message);
    }

    public MessageNotFoundException(String conversationId, Long userId) {
        super(3004, String.format("会话不存在: conversationId=%s, userId=%d", conversationId, userId));
    }
}