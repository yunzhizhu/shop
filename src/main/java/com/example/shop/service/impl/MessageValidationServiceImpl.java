package com.example.shop.service.impl;

import com.example.shop.dto.BatchNotificationRequest;
import com.example.shop.dto.SendMessageRequest;
import com.example.shop.dto.SystemNotificationRequest;
import com.example.shop.entity.User;
import com.example.shop.enums.MessageType;
import com.example.shop.exception.*;
import com.example.shop.mapper.UserMapper;
import com.example.shop.service.ConversationService;
import com.example.shop.service.MessageValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * 消息验证服务实现类
 * 负责消息系统的业务规则验证和权限验证
 */
@Slf4j
@Service
public class MessageValidationServiceImpl implements MessageValidationService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ConversationService conversationService;

    // URL格式验证正则表达式
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$",
        Pattern.CASE_INSENSITIVE
    );

    // 图片文件扩展名
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};

    @Override
    public void validateSendMessageRequest(SendMessageRequest request, Long senderId) {
        log.debug("验证发送消息请求: senderId={}, receiverId={}", senderId, request.getReceiverId());

        // 验证基本参数
        if (request == null) {
            throw new IllegalArgumentException("发送消息请求不能为空");
        }

        if (senderId == null) {
            throw new IllegalArgumentException("发送者ID不能为空");
        }

        if (request.getReceiverId() == null) {
            throw new IllegalArgumentException("接收者ID不能为空");
        }

        // 验证不能给自己发消息
        if (senderId.equals(request.getReceiverId())) {
            throw new SelfMessageException(senderId);
        }

        // 验证接收者是否存在
        if (!validateUserExists(request.getReceiverId())) {
            throw new InvalidReceiverException(request.getReceiverId());
        }

        // 验证消息内容
        validateMessageContent(request.getContent());

        // 验证消息类型
        if (request.getMessageType() == null) {
            request.setMessageType(MessageType.TEXT.getCode());
        }

        // 验证图片URL（如果是图片消息）
        validateImageUrl(request.getImageUrl(), request.getMessageType());

        log.debug("发送消息请求验证通过: senderId={}, receiverId={}", senderId, request.getReceiverId());
    }

    @Override
    public void validateSystemNotificationRequest(SystemNotificationRequest request) {
        log.debug("验证系统通知请求: receiverId={}", request.getReceiverId());

        // 验证基本参数
        if (request == null) {
            throw new IllegalArgumentException("系统通知请求不能为空");
        }

        if (request.getReceiverId() == null) {
            throw new IllegalArgumentException("接收者ID不能为空");
        }

        // 验证接收者是否存在
        if (!validateUserExists(request.getReceiverId())) {
            throw new InvalidReceiverException(request.getReceiverId());
        }

        // 验证通知内容
        validateMessageContent(request.getContent());

        log.debug("系统通知请求验证通过: receiverId={}", request.getReceiverId());
    }

    @Override
    public void validateBatchNotificationRequest(BatchNotificationRequest request) {
        log.debug("验证批量通知请求: userIds={}", request.getUserIds());

        // 验证基本参数
        if (request == null) {
            throw new IllegalArgumentException("批量通知请求不能为空");
        }

        // 验证通知内容
        validateMessageContent(request.getContent());

        // 如果指定了用户ID列表，验证用户是否存在
        if (!CollectionUtils.isEmpty(request.getUserIds())) {
            for (Long userId : request.getUserIds()) {
                if (userId == null) {
                    throw new IllegalArgumentException("用户ID不能为空");
                }
                if (!validateUserExists(userId)) {
                    throw new InvalidReceiverException(userId);
                }
            }
        }

        log.debug("批量通知请求验证通过: userIds={}", request.getUserIds());
    }

    @Override
    public boolean validateUserExists(Long userId) {
        if (userId == null || userId <= 0) {
            return false;
        }

        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return false;
            }
            
            // 检查用户状态，禁用的用户视为不存在
            if (user.getStatus() != null && user.getStatus().equals(0)) {
                log.warn("用户已被禁用: userId={}", userId);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("验证用户存在性时发生异常: userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public void validateConversationAccess(String conversationId, Long userId) {
        log.debug("验证会话访问权限: conversationId={}, userId={}", conversationId, userId);

        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException("会话ID不能为空");
        }

        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        try {
            // 委托给ConversationService进行权限验证
            conversationService.validateConversationAccess(conversationId, userId);
            log.debug("会话访问权限验证通过: conversationId={}, userId={}", conversationId, userId);
        } catch (Exception e) {
            log.error("会话访问权限验证失败: conversationId={}, userId={}, error={}", 
                    conversationId, userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public void validateImageUrl(String imageUrl, Integer messageType) {
        // 如果不是图片消息，不需要验证图片URL
        if (messageType == null || !MessageType.IMAGE.getCode().equals(messageType)) {
            return;
        }

        // 图片消息必须提供图片URL
        if (!StringUtils.hasText(imageUrl)) {
            throw new ImageUploadException("图片消息必须提供图片URL");
        }

        // 验证URL格式
        if (!isValidUrl(imageUrl)) {
            throw new ImageUploadException(imageUrl, "图片URL格式不正确");
        }

        // 验证是否为图片文件
        if (!isImageFile(imageUrl)) {
            throw new ImageUploadException(imageUrl, "URL不是有效的图片文件");
        }

        log.debug("图片URL验证通过: imageUrl={}", imageUrl);
    }

    @Override
    public void validateMessageContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new EmptyMessageContentException();
        }

        // 去除首尾空白字符后再次检查
        String trimmedContent = content.trim();
        if (trimmedContent.isEmpty()) {
            throw new EmptyMessageContentException("消息内容不能只包含空白字符");
        }

        // 验证消息长度（可根据需要调整）
        if (trimmedContent.length() > 5000) {
            throw new IllegalArgumentException("消息内容长度不能超过5000个字符");
        }

        log.debug("消息内容验证通过: contentLength={}", trimmedContent.length());
    }

    @Override
    public void validatePaginationParams(int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于0");
        }

        if (size < 1) {
            throw new IllegalArgumentException("页大小必须大于0");
        }

        if (size > 100) {
            throw new IllegalArgumentException("页大小不能超过100");
        }

        log.debug("分页参数验证通过: page={}, size={}", page, size);
    }

    @Override
    public void validateSearchKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }

        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            throw new IllegalArgumentException("搜索关键词不能只包含空白字符");
        }

        if (trimmedKeyword.length() > 50) {
            throw new IllegalArgumentException("搜索关键词长度不能超过50个字符");
        }

        log.debug("搜索关键词验证通过: keyword={}", trimmedKeyword);
    }

    /**
     * 验证URL格式是否正确
     */
    private boolean isValidUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }

        try {
            // 使用URL类进行基本格式验证
            new URL(url);
            // 使用正则表达式进行更严格的验证
            return URL_PATTERN.matcher(url).matches();
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * 验证是否为图片文件
     */
    private boolean isImageFile(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        for (String extension : IMAGE_EXTENSIONS) {
            if (lowerUrl.contains(extension)) {
                return true;
            }
        }

        return false;
    }
}