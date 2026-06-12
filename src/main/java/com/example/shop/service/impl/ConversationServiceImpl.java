package com.example.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.dto.ConversationListResponse;
import com.example.shop.dto.ConversationSearchResponse;
import com.example.shop.entity.Conversation;
import com.example.shop.entity.Message;
import com.example.shop.entity.User;
import com.example.shop.enums.ConversationType;
import com.example.shop.exception.BusinessException;
import com.example.shop.exception.ConversationAccessDeniedException;
import com.example.shop.exception.MessageNotFoundException;
import com.example.shop.mapper.ConversationMapper;
import com.example.shop.mapper.UserMapper;
import com.example.shop.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话服务实现类
 */
@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private UserMapper userMapper;

    @Value("${file.upload.server-base-url}")
    private String serverBaseUrl;

    @Override
    @Transactional
    @CacheEvict(value = {"conversationList", "totalUnreadCount", "privateUnreadCount", "systemUnreadCount", "conversationUnread"}, 
                allEntries = true)
    public void createOrUpdateConversation(String conversationId, Long userId, Message message) {
        log.info("创建或更新会话: conversationId={}, userId={}, messageId={}", 
                conversationId, userId, message.getMessageId());

        // 查询现有会话
        QueryWrapper<Conversation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId)
                   .eq("user_id", userId);
        Conversation conversation = conversationMapper.selectOne(queryWrapper);

        if (conversation == null) {
            // 创建新会话
            conversation = createNewConversation(conversationId, userId, message);
            // 使用自定义插入方法
            int result = conversationMapper.insertConversation(conversation);
            if (result > 0) {
                log.info("创建新会话成功: conversationId={}, userId={}", conversationId, userId);
            } else {
                log.error("创建新会话失败: conversationId={}, userId={}", conversationId, userId);
            }
        } else {
            // 更新现有会话
            updateExistingConversation(conversation, message);
            int result = conversationMapper.updateConversation(conversation);
            if (result > 0) {
                log.info("更新会话成功: conversationId={}, userId={}", conversationId, userId);
            } else {
                log.error("更新会话失败: conversationId={}, userId={}", conversationId, userId);
            }
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"conversationList", "totalUnreadCount", "privateUnreadCount", "systemUnreadCount", "conversationUnread"}, allEntries = true)
    public void updateUnreadCount(String conversationId, Long userId, int delta) {
        log.info("更新会话未读数: conversationId={}, userId={}, delta={}", 
                conversationId, userId, delta);

        int result = conversationMapper.updateUnreadCount(conversationId, userId, delta);
        if (result <= 0) {
            log.warn("更新会话未读数失败: conversationId={}, userId={}", conversationId, userId);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"conversationList", "totalUnreadCount", "privateUnreadCount", "systemUnreadCount", "conversationUnread"}, allEntries = true)
    public void resetUnreadCount(String conversationId, Long userId) {
        log.info("重置会话未读数: conversationId={}, userId={}", conversationId, userId);

        int result = conversationMapper.resetUnreadCount(conversationId, userId);
        if (result <= 0) {
            log.warn("重置会话未读数失败: conversationId={}, userId={}", conversationId, userId);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"conversationList", "totalUnreadCount", "privateUnreadCount", "systemUnreadCount", "conversationUnread"}, allEntries = true)
    public void decrementUnreadCount(String conversationId, Long userId) {
        log.info("减少会话未读数: conversationId={}, userId={}", conversationId, userId);

        int result = conversationMapper.updateUnreadCount(conversationId, userId, -1);
        if (result <= 0) {
            log.warn("减少会话未读数失败: conversationId={}, userId={}", conversationId, userId);
        }
    }

    @Override
    public String generateConversationId(Long userId1, Long userId2, ConversationType type) {
        switch (type) {
            case PRIVATE_CHAT:
                // 私信会话ID格式: private_{较小用户ID}_{较大用户ID}
                Long minUserId = Math.min(userId1, userId2);
                Long maxUserId = Math.max(userId1, userId2);
                return String.format("private_%d_%d", minUserId, maxUserId);
            
            case SYSTEM_NOTIFICATION:
                // 系统通知会话ID格式: system_{userId}
                return String.format("system_%d", userId1);
            
            default:
                throw new BusinessException(400, "不支持的会话类型");
        }
    }

    @Override
    public void validateConversationAccess(String conversationId, Long userId) {
        log.debug("验证会话访问权限: conversationId={}, userId={}", conversationId, userId);

        // 检查会话是否存在
        QueryWrapper<Conversation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId)
                   .eq("user_id", userId);
        Conversation conversation = conversationMapper.selectOne(queryWrapper);

        if (conversation == null) {
            throw new MessageNotFoundException(conversationId, userId);
        }

        // 验证用户是否有权限访问此会话
        if (!isUserAuthorizedForConversation(conversationId, userId)) {
            throw new ConversationAccessDeniedException(conversationId, userId);
        }
    }

    @Override
    @Cacheable(value = "conversationList", key = "#userId + '_' + #page + '_' + #size", 
               unless = "#result == null || #result.records.isEmpty()")
    public IPage<ConversationListResponse> getConversationList(Long userId, int page, int size) {
        log.info("获取用户会话列表: userId={}, page={}, size={}", userId, page, size);

        Page<Conversation> pageParam = new Page<>(page, size);
        IPage<Conversation> conversationPage = conversationMapper.selectConversationPage(pageParam, userId);

        // 转换为响应DTO
        IPage<ConversationListResponse> responsePage = new Page<>(page, size, conversationPage.getTotal());
        List<ConversationListResponse> responseList = conversationPage.getRecords().stream()
                .map(this::convertToConversationListResponse)
                .collect(Collectors.toList());
        responsePage.setRecords(responseList);

        return responsePage;
    }

    @Override
    public IPage<ConversationSearchResponse> searchConversations(Long userId, String keyword, int page, int size) {
        log.info("搜索用户会话: userId={}, keyword={}, page={}, size={}", userId, keyword, page, size);

        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(400, "搜索关键词不能为空");
        }

        Page<Conversation> pageParam = new Page<>(page, size);
        IPage<Conversation> conversationPage = conversationMapper.searchConversations(pageParam, userId, keyword);

        // 转换为响应DTO
        IPage<ConversationSearchResponse> responsePage = new Page<>(page, size, conversationPage.getTotal());
        List<ConversationSearchResponse> responseList = conversationPage.getRecords().stream()
                .map(this::convertToConversationSearchResponse)
                .collect(Collectors.toList());
        responsePage.setRecords(responseList);

        return responsePage;
    }

    @Override
    @Cacheable(value = "totalUnreadCount", key = "#userId")
    public Integer getTotalUnreadCount(Long userId) {
        log.debug("获取用户总未读数: userId={}", userId);
        
        Integer totalUnread = conversationMapper.selectTotalUnreadCount(userId);
        return totalUnread != null ? totalUnread : 0;
    }

    @Override
    @Cacheable(value = "privateUnreadCount", key = "#userId")
    public Integer getPrivateUnreadCount(Long userId) {
        log.debug("获取用户私信未读数: userId={}", userId);
        
        Integer privateUnread = conversationMapper.selectPrivateUnreadCount(userId);
        return privateUnread != null ? privateUnread : 0;
    }

    @Override
    @Cacheable(value = "systemUnreadCount", key = "#userId")
    public Integer getSystemUnreadCount(Long userId) {
        log.debug("获取用户系统通知未读数: userId={}", userId);
        
        Integer systemUnread = conversationMapper.selectSystemUnreadCount(userId);
        return systemUnread != null ? systemUnread : 0;
    }

    @Override
    @Cacheable(value = "conversationUnread", key = "#conversationId + '_' + #userId")
    public Integer getConversationUnreadCount(String conversationId, Long userId) {
        log.debug("获取会话未读数: conversationId={}, userId={}", conversationId, userId);
        
        Integer unreadCount = conversationMapper.selectConversationUnreadCount(conversationId, userId);
        return unreadCount != null ? unreadCount : 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"conversationList", "conversationInfo"}, allEntries = true)
    public void pinConversation(String conversationId, Long userId, boolean isPinned) {
        log.info("设置会话置顶状态: conversationId={}, userId={}, isPinned={}", 
                conversationId, userId, isPinned);

        // 验证会话访问权限
        validateConversationAccess(conversationId, userId);

        int pinnedValue = isPinned ? 1 : 0;
        int result = conversationMapper.updatePinnedStatus(conversationId, userId, pinnedValue);
        if (result <= 0) {
            throw new BusinessException("设置置顶状态失败");
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"conversationList", "conversationInfo", "totalUnreadCount", "privateUnreadCount", "systemUnreadCount", "conversationUnread"}, 
                allEntries = true)
    public void deleteConversation(String conversationId, Long userId) {
        log.info("删除会话: conversationId={}, userId={}", conversationId, userId);

        // 验证会话访问权限
        validateConversationAccess(conversationId, userId);

        // 执行软删除
        int result = conversationMapper.softDeleteConversation(conversationId, userId);
        if (result <= 0) {
            throw new BusinessException("删除会话失败");
        }
    }

    @Override
    @Cacheable(value = "conversationInfo", key = "#conversationId + '_' + #userId")
    public Conversation getConversationById(String conversationId, Long userId) {
        log.debug("获取会话信息: conversationId={}, userId={}", conversationId, userId);

        QueryWrapper<Conversation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId)
                   .eq("user_id", userId);
        return conversationMapper.selectOne(queryWrapper);
    }

    @Override
    public List<Conversation> getConversationsByIds(List<String> conversationIds, Long userId) {
        log.debug("批量获取会话信息: conversationIds={}, userId={}", conversationIds, userId);

        if (conversationIds == null || conversationIds.isEmpty()) {
            return List.of();
        }

        return conversationMapper.selectByConversationIds(conversationIds, userId);
    }

    /**
     * 创建新会话
     */
    private Conversation createNewConversation(String conversationId, Long userId, Message message) {
        Conversation conversation = new Conversation();
        conversation.setConversationId(conversationId);
        conversation.setUserId(userId);
        
        // 根据会话ID判断会话类型和目标信息
        if (conversationId.startsWith("private_")) {
            conversation.setConversationType(ConversationType.PRIVATE_CHAT.getCode());
            setPrivateChatTarget(conversation, userId, message);
        } else if (conversationId.startsWith("system_")) {
            conversation.setConversationType(ConversationType.SYSTEM_NOTIFICATION.getCode());
            setSystemNotificationTarget(conversation);
        }

        // 设置最后消息信息
        conversation.setLastMessageId(message.getMessageId());
        conversation.setLastMessageContent(message.getContent());
        conversation.setLastMessageTime(message.getCreatedAt());
        
        // 设置计数器
        conversation.setUnreadCount(message.getReceiverId().equals(userId) ? 1 : 0);
        conversation.setTotalCount(1);
        conversation.setIsPinned(0);
        
        return conversation;
    }

    /**
     * 更新现有会话
     */
    private void updateExistingConversation(Conversation conversation, Message message) {
        conversation.setLastMessageId(message.getMessageId());
        conversation.setLastMessageContent(message.getContent());
        conversation.setLastMessageTime(message.getCreatedAt());
        conversation.setTotalCount(conversation.getTotalCount() + 1);
        
        // 如果是接收的消息，增加未读数
        if (message.getReceiverId().equals(conversation.getUserId())) {
            conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        }
    }

    /**
     * 设置私信会话的目标信息
     */
    private void setPrivateChatTarget(Conversation conversation, Long userId, Message message) {
        // 确定对方用户ID
        Long targetUserId = message.getSenderId().equals(userId) ? 
                           message.getReceiverId() : message.getSenderId();
        
        // 查询对方用户信息
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser != null) {
            conversation.setTargetId(targetUserId);
            conversation.setTargetName(targetUser.getUsername());
            conversation.setTargetAvatar(targetUser.getAvatar());
        } else {
            conversation.setTargetId(targetUserId);
            conversation.setTargetName("未知用户");
            conversation.setTargetAvatar(null);
        }
    }

    /**
     * 设置系统通知会话的目标信息
     */
    private void setSystemNotificationTarget(Conversation conversation) {
        conversation.setTargetId(0L);
        conversation.setTargetName("系统通知");
        conversation.setTargetAvatar(null);
    }

    /**
     * 检查用户是否有权限访问会话
     */
    private boolean isUserAuthorizedForConversation(String conversationId, Long userId) {
        if (conversationId.startsWith("private_")) {
            // 私信会话：检查用户ID是否在会话ID中
            String[] parts = conversationId.split("_");
            if (parts.length == 3) {
                Long userId1 = Long.parseLong(parts[1]);
                Long userId2 = Long.parseLong(parts[2]);
                return userId.equals(userId1) || userId.equals(userId2);
            }
        } else if (conversationId.startsWith("system_")) {
            // 系统通知会话：检查用户ID是否匹配
            String[] parts = conversationId.split("_");
            if (parts.length == 2) {
                Long targetUserId = Long.parseLong(parts[1]);
                return userId.equals(targetUserId);
            }
        }
        return false;
    }

    /**
     * 转换为会话列表响应DTO
     */
    private ConversationListResponse convertToConversationListResponse(Conversation conversation) {
        ConversationListResponse response = new ConversationListResponse();
        BeanUtils.copyProperties(conversation, response);
        
        // 转换头像URL为完整路径
        if (StringUtils.hasText(conversation.getTargetAvatar())) {
            response.setTargetAvatar(buildFullImageUrl(conversation.getTargetAvatar()));
        }
        
        return response;
    }

    /**
     * 转换为会话搜索响应DTO
     */
    private ConversationSearchResponse convertToConversationSearchResponse(Conversation conversation) {
        ConversationSearchResponse response = new ConversationSearchResponse();
        BeanUtils.copyProperties(conversation, response);
        
        // 转换头像URL为完整路径
        if (StringUtils.hasText(conversation.getTargetAvatar())) {
            response.setTargetAvatar(buildFullImageUrl(conversation.getTargetAvatar()));
        }
        
        return response;
    }

    /**
     * 构建完整的图片URL
     * 如果已经是完整URL则直接返回，否则拼接服务器基础URL
     */
    private String buildFullImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return imagePath;
        }
        
        // 如果已经是完整URL（以http://或https://开头），直接返回
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }
        
        // 拼接服务器基础URL
        return serverBaseUrl + imagePath;
    }
}