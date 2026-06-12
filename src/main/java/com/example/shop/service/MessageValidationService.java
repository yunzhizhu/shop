package com.example.shop.service;

import com.example.shop.dto.SendMessageRequest;
import com.example.shop.dto.SystemNotificationRequest;
import com.example.shop.dto.BatchNotificationRequest;

/**
 * 消息验证服务接口
 * 负责消息系统的业务规则验证
 */
public interface MessageValidationService {

    /**
     * 验证发送私信请求
     * @param request 发送消息请求
     * @param senderId 发送者ID
     */
    void validateSendMessageRequest(SendMessageRequest request, Long senderId);

    /**
     * 验证发送系统通知请求
     * @param request 系统通知请求
     */
    void validateSystemNotificationRequest(SystemNotificationRequest request);

    /**
     * 验证批量发送通知请求
     * @param request 批量通知请求
     */
    void validateBatchNotificationRequest(BatchNotificationRequest request);

    /**
     * 验证用户是否存在
     * @param userId 用户ID
     * @return 用户是否存在
     */
    boolean validateUserExists(Long userId);

    /**
     * 验证会话访问权限
     * @param conversationId 会话ID
     * @param userId 用户ID
     */
    void validateConversationAccess(String conversationId, Long userId);

    /**
     * 验证图片URL的有效性
     * @param imageUrl 图片URL
     * @param messageType 消息类型
     */
    void validateImageUrl(String imageUrl, Integer messageType);

    /**
     * 验证消息内容
     * @param content 消息内容
     */
    void validateMessageContent(String content);

    /**
     * 验证分页参数
     * @param page 页码
     * @param size 页大小
     */
    void validatePaginationParams(int page, int size);

    /**
     * 验证搜索关键词
     * @param keyword 搜索关键词
     */
    void validateSearchKeyword(String keyword);
}