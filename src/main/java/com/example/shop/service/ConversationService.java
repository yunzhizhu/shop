package com.example.shop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.dto.ConversationListResponse;
import com.example.shop.dto.ConversationSearchResponse;
import com.example.shop.entity.Conversation;
import com.example.shop.entity.Message;
import com.example.shop.enums.ConversationType;

import java.util.List;

/**
 * 会话服务接口
 */
public interface ConversationService {

    /**
     * 创建或更新会话
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param message 消息对象
     */
    void createOrUpdateConversation(String conversationId, Long userId, Message message);

    /**
     * 更新会话未读数
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param delta 变化量（正数增加，负数减少）
     */
    void updateUnreadCount(String conversationId, Long userId, int delta);

    /**
     * 重置会话未读数为0
     * @param conversationId 会话ID
     * @param userId 用户ID
     */
    void resetUnreadCount(String conversationId, Long userId);

    /**
     * 减少会话未读数（减1）
     * @param conversationId 会话ID
     * @param userId 用户ID
     */
    void decrementUnreadCount(String conversationId, Long userId);

    /**
     * 生成会话ID
     * @param userId1 用户1ID
     * @param userId2 用户2ID（系统通知时为0）
     * @param type 会话类型
     * @return 会话ID
     */
    String generateConversationId(Long userId1, Long userId2, ConversationType type);

    /**
     * 验证用户是否有权限访问会话
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @throws RuntimeException 如果无权限访问
     */
    void validateConversationAccess(String conversationId, Long userId);

    /**
     * 获取用户会话列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 会话列表
     */
    IPage<ConversationListResponse> getConversationList(Long userId, int page, int size);

    /**
     * 搜索用户会话
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    IPage<ConversationSearchResponse> searchConversations(Long userId, String keyword, int page, int size);

    /**
     * 获取用户总未读数
     * @param userId 用户ID
     * @return 总未读数
     */
    Integer getTotalUnreadCount(Long userId);

    /**
     * 获取用户私信未读数
     * @param userId 用户ID
     * @return 私信未读数
     */
    Integer getPrivateUnreadCount(Long userId);

    /**
     * 获取用户系统通知未读数
     * @param userId 用户ID
     * @return 系统通知未读数
     */
    Integer getSystemUnreadCount(Long userId);

    /**
     * 获取指定会话的未读数
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 会话未读数
     */
    Integer getConversationUnreadCount(String conversationId, Long userId);

    /**
     * 设置会话置顶状态
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param isPinned 是否置顶
     */
    void pinConversation(String conversationId, Long userId, boolean isPinned);

    /**
     * 删除会话（软删除）
     * @param conversationId 会话ID
     * @param userId 用户ID
     */
    void deleteConversation(String conversationId, Long userId);

    /**
     * 根据会话ID获取会话信息
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 会话信息
     */
    Conversation getConversationById(String conversationId, Long userId);

    /**
     * 批量获取会话信息
     * @param conversationIds 会话ID列表
     * @param userId 用户ID
     * @return 会话列表
     */
    List<Conversation> getConversationsByIds(List<String> conversationIds, Long userId);
}