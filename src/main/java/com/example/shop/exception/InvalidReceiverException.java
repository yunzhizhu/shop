package com.example.shop.exception;

/**
 * 接收者无效异常
 */
public class InvalidReceiverException extends BusinessException {

    public InvalidReceiverException() {
        super(3001, "接收者不存在");
    }

    public InvalidReceiverException(String message) {
        super(3001, message);
    }

    public InvalidReceiverException(Long receiverId) {
        super(3001, String.format("接收者不存在: receiverId=%d", receiverId));
    }
}