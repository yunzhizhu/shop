package com.example.shop.exception;

import com.example.shop.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 消息系统专用异常处理器
 * 处理消息相关的业务异常，提供更精确的错误信息
 */
@Slf4j
@RestControllerAdvice
@Order(1) // 优先级高于全局异常处理器
public class MessageExceptionHandler {

    /**
     * 处理消息不存在异常
     */
    @ExceptionHandler(MessageNotFoundException.class)
    public Result<Void> handleMessageNotFound(MessageNotFoundException e, HttpServletRequest request) {
        log.error("消息不存在异常: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理会话访问权限不足异常
     */
    @ExceptionHandler(ConversationAccessDeniedException.class)
    public Result<Void> handleConversationAccessDenied(ConversationAccessDeniedException e, HttpServletRequest request) {
        log.error("会话访问权限不足异常: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理接收者无效异常
     */
    @ExceptionHandler(InvalidReceiverException.class)
    public Result<Void> handleInvalidReceiver(InvalidReceiverException e, HttpServletRequest request) {
        log.error("接收者无效异常: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理不能给自己发消息异常
     */
    @ExceptionHandler(SelfMessageException.class)
    public Result<Void> handleSelfMessage(SelfMessageException e, HttpServletRequest request) {
        log.error("不能给自己发消息异常: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理消息内容为空异常
     */
    @ExceptionHandler(EmptyMessageContentException.class)
    public Result<Void> handleEmptyMessageContent(EmptyMessageContentException e, HttpServletRequest request) {
        log.error("消息内容为空异常: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理图片上传异常
     */
    @ExceptionHandler(ImageUploadException.class)
    public Result<Void> handleImageUpload(ImageUploadException e, HttpServletRequest request) {
        log.error("图片上传异常: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
}