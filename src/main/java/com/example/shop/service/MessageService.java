package com.example.shop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.dto.*;
import com.example.shop.entity.Message;

import java.util.List;

/**
 * 消息服务接口
 */
public interface MessageService {

    // ========== 用户消息功能 ==========
    
    /**
     * 发送私信
     */
    Long sendMessage(SendMessageRequest request);

    /**
     * 获取总未读消息数
     */
    TotalUnreadResponse getTotalUnreadCount();

    /**
     * 获取会话列表
     */
    IPage<ConversationListResponse> getConversationList(int page, int size);

    /**
     * 获取消息历史记录
     */
    IPage<MessageHistoryResponse> getMessageHistory(String conversationId, int page, int size);

    /**
     * 标记会话为已读
     */
    void markConversationAsRead(ConversationReadRequest request);

    /**
     * 标记单条消息为已读
     */
    void markSingleMessageAsRead(Long messageId);

    /**
     * 设置会话置顶状态
     */
    void pinConversation(ConversationPinRequest request);

    /**
     * 删除会话
     */
    void deleteConversation(ConversationDeleteRequest request);

    /**
     * 搜索会话
     */
    IPage<ConversationSearchResponse> searchConversations(String keyword, int page, int size);

    // ========== 管理员功能 ==========

    /**
     * 发送系统通知
     */
    Long sendSystemNotification(SystemNotificationRequest request);

    /**
     * 批量发送系统通知
     */
    BatchSendResult batchSendNotification(BatchNotificationRequest request);

    /**
     * 获取系统通知列表（管理员）
     */
    IPage<MessageHistoryResponse> getAdminNotifications(Integer contentType, int page, int size);

    // ========== 已废弃的方法（保留兼容性） ==========

    /**
     * 获取消息列表
     * @deprecated 使用 getConversationList 替代
     */
    @Deprecated
    List<MessageListResponse> getMessageList();

    /**
     * 获取聊天历史记录
     * @deprecated 使用 getMessageHistory 替代
     */
    @Deprecated
    IPage<MessageHistoryResponse> getChatHistory(Long chatUserId, int page, int size);

    /**
     * 标记消息为已读
     * @deprecated 使用 markConversationAsRead 替代
     */
    @Deprecated
    void markAsRead(Long senderId);

    /**
     * 获取未读消息数
     * @deprecated 使用 getTotalUnreadCount 替代
     */
    @Deprecated
    Integer getUnreadCount(Long senderId);
}
