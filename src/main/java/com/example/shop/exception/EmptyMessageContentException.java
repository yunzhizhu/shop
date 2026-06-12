package com.example.shop.exception;

/**
 * 消息内容为空异常
 */
public class EmptyMessageContentException extends BusinessException {

    public EmptyMessageContentException() {
        super(3003, "消息内容不能为空");
    }

    public EmptyMessageContentException(String message) {
        super(3003, message);
    }
}