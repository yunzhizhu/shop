package com.example.shop.exception;

/**
 * 不能给自己发消息异常
 */
public class SelfMessageException extends BusinessException {

    public SelfMessageException() {
        super(3002, "不能给自己发消息");
    }

    public SelfMessageException(String message) {
        super(3002, message);
    }

    public SelfMessageException(Long userId) {
        super(3002, String.format("不能给自己发消息: userId=%d", userId));
    }
}