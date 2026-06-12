package com.example.shop.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.dto.*;
import com.example.shop.entity.Message;
import com.example.shop.entity.User;
import com.example.shop.enums.ContentType;
import com.example.shop.enums.ConversationType;
import com.example.shop.enums.MessageType;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.MessageMapper;
import com.example.shop.mapper.UserMapper;
import com.example.shop.service.ConversationService;
import com.example.shop.service.FriendService;
import com.example.shop.service.MessageService;
import com.example.shop.service.MessageValidationService;
import com.example.shop.service.WebSocketService;
import com.example.shop.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 消息服务实现类
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private MessageValidationService messageValidationService;

    @Autowired
    private FriendService friendService;

    @Autowired
    private com.example.shop.helper.FileUploadHelper fileUploadHelper;

    // ========== 新的基于会话的消息功能 ==========

    @Override
    @Transactional
    public Long sendMessage(SendMessageRequest request) {
        Long senderId = SecurityUtil.getCurrentUserId();

        // 使用统一的验证服务进行验证
        messageValidationService.validateSendMessageRequest(request, senderId);

        // 生成会话ID
        String conversationId = conversationService.generateConversationId(
            senderId, request.getReceiverId(), ConversationType.PRIVATE_CHAT);

        // 创建消息
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setReceiverId(request.getReceiverId());
        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType());
        message.setContentType(request.getContentType() != null ? request.getContentType() : ContentType.PRIVATE_MESSAGE.getCode());
        message.setImageUrl(request.getImageUrl());
        message.setIsRead(0);

        int result = messageMapper.insert(message);
        if (result <= 0) {
            throw new BusinessException("发送消息失败");
        }

        // 更新或创建会话记录（发送者和接收者都需要）
        conversationService.createOrUpdateConversation(conversationId, senderId, message);
        conversationService.createOrUpdateConversation(conversationId, request.getReceiverId(), message);

        // 推送实时消息给接收者
        try {
            WebSocketMessageDTO messageDTO = buildWebSocketMessageDTO(message);
            webSocketService.pushMessage(request.getReceiverId(), messageDTO);
            
            // 推送未读数更新给接收者
            Integer totalUnreadCount = conversationService.getTotalUnreadCount(request.getReceiverId());
            Integer conversationUnreadCount = conversationService.getConversationUnreadCount(conversationId, request.getReceiverId());
            UnreadUpdateDTO unreadUpdateDTO = new UnreadUpdateDTO(conversationId, conversationUnreadCount, totalUnreadCount);
            webSocketService.pushUnreadUpdate(request.getReceiverId(), unreadUpdateDTO);
            
        } catch (Exception e) {
            log.warn("WebSocket推送失败，但消息发送成功: messageId={}, error={}", message.getMessageId(), e.getMessage());
        }

        log.info("用户发送消息成功: senderId={}, receiverId={}, messageId={}, conversationId={}", 
                senderId, request.getReceiverId(), message.getMessageId(), conversationId);
        
        return message.getMessageId();
    }

    @Override
    public TotalUnreadResponse getTotalUnreadCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        Integer totalUnread = conversationService.getTotalUnreadCount(userId);
        Integer privateUnread = conversationService.getPrivateUnreadCount(userId);
        Integer systemUnread = conversationService.getSystemUnreadCount(userId);
        Integer friendRequestCount = friendService.getPendingRequestCount(userId);
        
        // 总未读数 = 消息未读数 + 好友请求数
        Integer totalWithFriendRequests = totalUnread + friendRequestCount;
        
        return new TotalUnreadResponse(totalWithFriendRequests, privateUnread, systemUnread, friendRequestCount);
    }

    @Override
    public IPage<ConversationListResponse> getConversationList(int page, int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 使用统一的验证服务进行验证
        messageValidationService.validatePaginationParams(page, size);
        
        return conversationService.getConversationList(userId, page, size);
    }

    @Override
    public IPage<MessageHistoryResponse> getMessageHistory(String conversationId, int page, int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 使用统一的验证服务进行验证
        messageValidationService.validatePaginationParams(page, size);
        messageValidationService.validateConversationAccess(conversationId, userId);
        
        Page<MessageHistoryResponse> pageParam = new Page<>(page, size);
        IPage<MessageHistoryResponse> historyPage = messageMapper.selectMessageHistoryByConversation(pageParam, conversationId);
        
        // 填充发送者信息和图片URL
        historyPage.getRecords().forEach(this::fillMessageDetails);
        
        return historyPage;
    }

    @Override
    @Transactional
    public void markConversationAsRead(ConversationReadRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        String conversationId = request.getConversationId();
        
        // 使用统一的验证服务进行验证
        messageValidationService.validateConversationAccess(conversationId, userId);
        
        // 标记会话中的所有未读消息为已读
        int result = messageMapper.markConversationAsRead(conversationId, userId);
        
        // 重置会话未读数
        conversationService.resetUnreadCount(conversationId, userId);
        
        // 推送未读数更新
        try {
            Integer totalUnreadCount = conversationService.getTotalUnreadCount(userId);
            UnreadUpdateDTO unreadUpdateDTO = new UnreadUpdateDTO(conversationId, 0, totalUnreadCount);
            webSocketService.pushUnreadUpdate(userId, unreadUpdateDTO);
        } catch (Exception e) {
            log.warn("推送未读数更新失败: userId={}, conversationId={}, error={}", userId, conversationId, e.getMessage());
        }
        
        log.info("标记会话已读: userId={}, conversationId={}, count={}", userId, conversationId, result);
    }

    @Override
    @Transactional
    public void markSingleMessageAsRead(Long messageId) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 验证消息ID
        if (messageId == null || messageId <= 0) {
            throw new BusinessException("消息ID不能为空");
        }
        
        // 查询消息信息
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }
        
        // 验证消息接收者
        if (!message.getReceiverId().equals(userId)) {
            throw new BusinessException("无权操作此消息");
        }
        
        // 如果消息已读，直接返回
        if (message.getIsRead() == 1) {
            log.info("消息已是已读状态: messageId={}, userId={}", messageId, userId);
            return;
        }
        
        // 标记消息为已读
        int result = messageMapper.markSingleMessageAsRead(messageId, userId);
        
        if (result > 0) {
            // 更新会话未读数（减1）
            String conversationId = message.getConversationId();
            conversationService.decrementUnreadCount(conversationId, userId);
            
            // 推送未读数更新
            try {
                Integer totalUnreadCount = conversationService.getTotalUnreadCount(userId);
                Integer conversationUnreadCount = conversationService.getConversationUnreadCount(conversationId, userId);
                UnreadUpdateDTO unreadUpdateDTO = new UnreadUpdateDTO(conversationId, conversationUnreadCount, totalUnreadCount);
                webSocketService.pushUnreadUpdate(userId, unreadUpdateDTO);
            } catch (Exception e) {
                log.warn("推送未读数更新失败: userId={}, messageId={}, error={}", userId, messageId, e.getMessage());
            }
            
            log.info("标记单条消息已读成功: messageId={}, userId={}, conversationId={}", messageId, userId, conversationId);
        } else {
            log.warn("标记单条消息已读失败: messageId={}, userId={}", messageId, userId);
        }
    }

    @Override
    @Transactional
    public void pinConversation(ConversationPinRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        String conversationId = request.getConversationId();
        
        // 使用统一的验证服务进行验证
        messageValidationService.validateConversationAccess(conversationId, userId);
        
        // 设置会话置顶状态
        conversationService.pinConversation(conversationId, userId, request.getIsPinned());
        
        log.info("设置会话置顶: userId={}, conversationId={}, isPinned={}", 
                userId, conversationId, request.getIsPinned());
    }

    @Override
    @Transactional
    public void deleteConversation(ConversationDeleteRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        String conversationId = request.getConversationId();
        
        // 使用统一的验证服务进行验证
        messageValidationService.validateConversationAccess(conversationId, userId);
        
        // 软删除会话
        conversationService.deleteConversation(conversationId, userId);
        
        log.info("删除会话: userId={}, conversationId={}", userId, conversationId);
    }

    @Override
    public IPage<ConversationSearchResponse> searchConversations(String keyword, int page, int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 使用统一的验证服务进行验证
        messageValidationService.validateSearchKeyword(keyword);
        messageValidationService.validatePaginationParams(page, size);
        
        return conversationService.searchConversations(userId, keyword, page, size);
    }

    // ========== 管理员功能 ==========

    @Override
    @Transactional
    public Long sendSystemNotification(SystemNotificationRequest request) {
        Long senderId = SecurityUtil.getCurrentUserId();

        // 使用统一的验证服务进行验证
        messageValidationService.validateSystemNotificationRequest(request);

        // 生成系统通知会话ID
        String conversationId = conversationService.generateConversationId(
            request.getReceiverId(), 0L, ConversationType.SYSTEM_NOTIFICATION);

        // 创建系统通知消息
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(0L); // 系统消息发送者ID为0
        message.setReceiverId(request.getReceiverId());
        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType());
        message.setContentType(request.getContentType());
        message.setImageUrl(request.getImageUrl());
        message.setIsRead(0);

        int result = messageMapper.insert(message);
        if (result <= 0) {
            throw new BusinessException("发送系统通知失败");
        }

        // 创建或更新系统通知会话
        conversationService.createOrUpdateConversation(conversationId, request.getReceiverId(), message);

        // 推送系统通知给接收者
        try {
            WebSocketMessageDTO messageDTO = buildWebSocketMessageDTO(message);
            webSocketService.pushMessage(request.getReceiverId(), messageDTO);
            
            // 推送未读数更新给接收者
            Integer totalUnreadCount = conversationService.getTotalUnreadCount(request.getReceiverId());
            Integer conversationUnreadCount = conversationService.getConversationUnreadCount(conversationId, request.getReceiverId());
            UnreadUpdateDTO unreadUpdateDTO = new UnreadUpdateDTO(conversationId, conversationUnreadCount, totalUnreadCount);
            webSocketService.pushUnreadUpdate(request.getReceiverId(), unreadUpdateDTO);
            
        } catch (Exception e) {
            log.warn("WebSocket推送系统通知失败，但消息发送成功: messageId={}, error={}", message.getMessageId(), e.getMessage());
        }

        log.info("管理员发送系统通知成功: senderId={}, receiverId={}, messageId={}, conversationId={}", 
                senderId, request.getReceiverId(), message.getMessageId(), conversationId);
        
        return message.getMessageId();
    }

    @Override
    @Transactional
    public BatchSendResult batchSendNotification(BatchNotificationRequest request) {
        Long senderId = SecurityUtil.getCurrentUserId();
        
        // 使用统一的验证服务进行验证
        messageValidationService.validateBatchNotificationRequest(request);

        List<Long> targetUserIds = request.getUserIds();
        
        // 如果没有指定用户ID，则发送给所有活跃用户
        if (CollectionUtils.isEmpty(targetUserIds)) {
            targetUserIds = userMapper.selectAllActiveUserIds();
        }

        BatchSendResult result = new BatchSendResult();
        int successCount = 0;
        int failedCount = 0;

        for (Long userId : targetUserIds) {
            try {
                // 验证用户是否存在
                User user = userMapper.selectById(userId);
                if (user == null) {
                    failedCount++;
                    continue;
                }

                // 生成系统通知会话ID
                String conversationId = conversationService.generateConversationId(
                    userId, 0L, ConversationType.SYSTEM_NOTIFICATION);

                // 创建系统通知消息
                Message message = new Message();
                message.setConversationId(conversationId);
                message.setSenderId(0L); // 系统消息发送者ID为0
                message.setReceiverId(userId);
                message.setContent(request.getContent());
                message.setMessageType(request.getMessageType());
                message.setContentType(request.getContentType());
                message.setImageUrl(request.getImageUrl());
                message.setIsRead(0);

                int insertResult = messageMapper.insert(message);
                if (insertResult > 0) {
                    // 创建或更新系统通知会话
                    conversationService.createOrUpdateConversation(conversationId, userId, message);
                    
                    // 推送系统通知给用户
                    try {
                        WebSocketMessageDTO messageDTO = buildWebSocketMessageDTO(message);
                        webSocketService.pushMessage(userId, messageDTO);
                        
                        // 推送未读数更新
                        Integer totalUnreadCount = conversationService.getTotalUnreadCount(userId);
                        Integer conversationUnreadCount = conversationService.getConversationUnreadCount(conversationId, userId);
                        UnreadUpdateDTO unreadUpdateDTO = new UnreadUpdateDTO(conversationId, conversationUnreadCount, totalUnreadCount);
                        webSocketService.pushUnreadUpdate(userId, unreadUpdateDTO);
                        
                    } catch (Exception e) {
                        log.warn("WebSocket推送批量通知失败: userId={}, messageId={}, error={}", userId, message.getMessageId(), e.getMessage());
                    }
                    
                    successCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("批量发送通知失败: userId={}, error={}", userId, e.getMessage());
                failedCount++;
            }
        }

        result.setSuccessCount(successCount);
        result.setFailedCount(failedCount);
        result.setTotalCount(successCount + failedCount);
        result.setMessage(String.format("批量发送完成，成功：%d，失败：%d", successCount, failedCount));

        log.info("批量发送系统通知完成: senderId={}, successCount={}, failedCount={}", 
                senderId, successCount, failedCount);
        
        return result;
    }

    @Override
    public IPage<MessageHistoryResponse> getAdminNotifications(Integer contentType, int page, int size) {
        Page<MessageHistoryResponse> pageParam = new Page<>(page, size);
        
        IPage<MessageHistoryResponse> notificationPage = messageMapper.selectAdminNotifications(pageParam, contentType);
        
        // 填充图片URL和接收者数量
        notificationPage.getRecords().forEach(message -> {
            // 转换图片URL为完整URL
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                message.setImageUrl(fileUploadHelper.toFullUrl(message.getImageUrl()));
            }
            
            // 填充图片URL列表（用于兼容）
            fillImageUrls(message);
            
            // receiverId字段在分组查询中存储的是接收者数量
            if (message.getReceiverId() != null) {
                message.setReceiverCount(message.getReceiverId().intValue());
            }
        });
        
        log.info("管理员获取系统通知列表: contentType={}, page={}, size={}, total={}", 
                contentType, page, size, notificationPage.getTotal());
        
        return notificationPage;
    }

    // ========== 已废弃的方法（保留兼容性） ==========

    @Override
    @Deprecated
    public List<MessageListResponse> getMessageList() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<MessageListResponse> messageList = messageMapper.selectMessageList(userId);

        // 设置未读消息数
        messageList.forEach(item -> {
            if (item.getIsSystem()) {
                // 系统消息 - 暂时设为0，因为SystemNotice表不存在
                item.setUnreadCount(0);
            } else {
                // 私信
                item.setUnreadCount(getUnreadCount(item.getSenderId()));
            }
        });

        return messageList;
    }

    @Override
    @Deprecated
    public IPage<MessageHistoryResponse> getChatHistory(Long chatUserId, int page, int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        Page<MessageHistoryResponse> pageParam = new Page<>(page, size);
        
        IPage<MessageHistoryResponse> historyPage = messageMapper.selectMessageHistory(pageParam, userId, chatUserId);
        
        // 填充图片URL
        historyPage.getRecords().forEach(this::fillImageUrls);
        
        return historyPage;
    }

    @Override
    @Deprecated
    @Transactional
    public void markAsRead(Long senderId) {
        Long userId = SecurityUtil.getCurrentUserId();
        int result = messageMapper.markAsRead(userId, senderId);
        log.info("标记消息已读: userId={}, senderId={}, count={}", userId, senderId, result);
    }

    @Override
    @Deprecated
    public Integer getUnreadCount(Long senderId) {
        Long userId = SecurityUtil.getCurrentUserId();
        Integer count = messageMapper.getUnreadCount(userId, senderId);
        return count != null ? count : 0;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 填充消息详细信息
     */
    private void fillMessageDetails(MessageHistoryResponse message) {
        // 填充发送者信息
        if (message.getSenderId() != null && message.getSenderId() > 0) {
            User sender = userMapper.selectById(message.getSenderId());
            if (sender != null) {
                message.setSenderName(sender.getUsername());
                // 转换头像为完整URL
                String avatar = sender.getAvatar();
                if (avatar != null && !avatar.isEmpty()) {
                    avatar = fileUploadHelper.toFullUrl(avatar);
                }
                message.setSenderAvatar(avatar);
            }
        } else {
            // 系统消息
            message.setSenderName("系统");
            message.setSenderAvatar(null);
            message.setIsSystem(true);
        }

        // 填充图片URL
        fillImageUrls(message);
    }

    /**
     * 填充消息的图片URL
     */
    private void fillImageUrls(MessageHistoryResponse message) {
        if (MessageType.IMAGE.getCode().equals(message.getMessageType())) {
            // 图片URL现在直接存储在message的imageUrl字段中
            if (message.getImageUrl() != null) {
                // 转换为完整URL
                String fullImageUrl = fileUploadHelper.toFullUrl(message.getImageUrl());
                message.setImageUrl(fullImageUrl);
                message.setImageUrls(List.of(fullImageUrl));
            }
        }
    }

    /**
     * 构建WebSocket消息DTO
     */
    private WebSocketMessageDTO buildWebSocketMessageDTO(Message message) {
        WebSocketMessageDTO messageDTO = new WebSocketMessageDTO();
        
        // 基本消息信息
        messageDTO.setMessageId(message.getMessageId());
        messageDTO.setConversationId(message.getConversationId());
        messageDTO.setSenderId(message.getSenderId());
        messageDTO.setReceiverId(message.getReceiverId());
        messageDTO.setContent(message.getContent());
        messageDTO.setMessageType(message.getMessageType());
        messageDTO.setContentType(message.getContentType());
        // 转换图片URL为完整URL
        if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
            messageDTO.setImageUrl(fileUploadHelper.toFullUrl(message.getImageUrl()));
        } else {
            messageDTO.setImageUrl(message.getImageUrl());
        }
        messageDTO.setCreatedAt(message.getCreatedAt());
        
        // 设置发送者信息
        if (message.getSenderId() != null && message.getSenderId() > 0) {
            User sender = userMapper.selectById(message.getSenderId());
            if (sender != null) {
                messageDTO.setSenderName(sender.getUsername());
                // 转换头像为完整URL
                String avatar = sender.getAvatar();
                if (avatar != null && !avatar.isEmpty()) {
                    avatar = fileUploadHelper.toFullUrl(avatar);
                }
                messageDTO.setSenderAvatar(avatar);
                messageDTO.setIsSystem(false);
            }
        } else {
            // 系统消息
            messageDTO.setSenderName("系统通知");
            messageDTO.setSenderAvatar(null);
            messageDTO.setIsSystem(true);
        }
        
        return messageDTO;
    }
}