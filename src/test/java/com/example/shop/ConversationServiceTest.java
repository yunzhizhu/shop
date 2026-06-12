package com.example.shop;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.shop.entity.Conversation;
import com.example.shop.entity.Message;
import com.example.shop.entity.User;
import com.example.shop.enums.ConversationType;
import com.example.shop.enums.MessageType;
import com.example.shop.enums.ContentType;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.ConversationMapper;
import com.example.shop.mapper.UserMapper;
import com.example.shop.service.ConversationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConversationService测试类
 * 验证会话管理服务的核心功能
 */
@SpringBootTest
@Transactional
public class ConversationServiceTest {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 测试会话ID生成逻辑
     * Requirements: 18.1, 18.2, 18.3
     */
    @Test
    public void testGenerateConversationId() {
        // 测试私信会话ID生成 - 较小用户ID在前
        String privateId1 = conversationService.generateConversationId(100L, 200L, ConversationType.PRIVATE_CHAT);
        assertEquals("private_100_200", privateId1);

        // 测试私信会话ID生成 - 用户ID顺序相反
        String privateId2 = conversationService.generateConversationId(200L, 100L, ConversationType.PRIVATE_CHAT);
        assertEquals("private_100_200", privateId2);

        // 验证同一对用户的会话ID一致性
        assertEquals(privateId1, privateId2);

        // 测试系统通知会话ID生成
        String systemId = conversationService.generateConversationId(100L, 0L, ConversationType.SYSTEM_NOTIFICATION);
        assertEquals("system_100", systemId);
    }

    /**
     * 测试创建或更新会话功能
     * Requirements: 18.4, 19.1
     */
    @Test
    public void testCreateOrUpdateConversation() {
        // 创建测试用户
        User sender = createTestUser(100L, "sender");
        User receiver = createTestUser(200L, "receiver");

        // 创建测试消息
        Message message = createTestMessage(100L, 200L, "Hello World");
        String conversationId = "private_100_200";

        // 测试创建新会话
        conversationService.createOrUpdateConversation(conversationId, 200L, message);

        // 验证会话是否创建成功
        Conversation conversation = conversationService.getConversationById(conversationId, 200L);
        assertNotNull(conversation);
        assertEquals(conversationId, conversation.getConversationId());
        assertEquals(200L, conversation.getUserId());
        assertEquals(100L, conversation.getTargetId()); // 这里应该是发送者ID
        assertEquals("sender", conversation.getTargetName());
        assertEquals(1, conversation.getUnreadCount()); // 接收者的未读数应该为1
        assertEquals(1, conversation.getTotalCount());

        // 测试更新现有会话
        Message message2 = createTestMessage(200L, 100L, "Hello back");
        conversationService.createOrUpdateConversation(conversationId, 200L, message2);

        // 验证会话更新
        conversation = conversationService.getConversationById(conversationId, 200L);
        assertEquals(2, conversation.getTotalCount());
        assertEquals("Hello back", conversation.getLastMessageContent());
        assertEquals(1, conversation.getUnreadCount()); // 用户200仍有1条未读消息（来自用户100的第一条消息）
    }

    /**
     * 测试未读数统计和更新
     * Requirements: 19.1, 19.2, 19.3
     */
    @Test
    public void testUnreadCountOperations() {
        String conversationId = "private_100_200";
        Long userId = 200L;

        // 创建测试会话
        createTestConversation(conversationId, userId);

        // 测试增加未读数
        conversationService.updateUnreadCount(conversationId, userId, 3);
        Conversation conversation = conversationMapper.selectOne(
            new QueryWrapper<Conversation>()
                .eq("conversation_id", conversationId)
                .eq("user_id", userId)
        );
        assertEquals(3, conversation.getUnreadCount());

        // 测试减少未读数
        conversationService.updateUnreadCount(conversationId, userId, -1);
        conversation = conversationMapper.selectOne(
            new QueryWrapper<Conversation>()
                .eq("conversation_id", conversationId)
                .eq("user_id", userId)
        );
        assertEquals(2, conversation.getUnreadCount());

        // 测试重置未读数
        conversationService.resetUnreadCount(conversationId, userId);
        conversation = conversationMapper.selectOne(
            new QueryWrapper<Conversation>()
                .eq("conversation_id", conversationId)
                .eq("user_id", userId)
        );
        assertEquals(0, conversation.getUnreadCount());
    }

    /**
     * 测试总未读数统计
     * Requirements: 19.3
     */
    @Test
    public void testGetTotalUnreadCount() {
        Long userId = 300L;

        // 创建多个会话，使用不同的target_id避免唯一约束冲突
        createTestConversation("private_300_400", userId, 5, 400L);
        createTestConversation("private_300_500", userId, 3, 500L);
        createTestConversation("system_300", userId, 2, 0L);

        // 测试总未读数
        Integer totalUnread = conversationService.getTotalUnreadCount(userId);
        assertEquals(10, totalUnread);

        // 测试没有未读消息的用户
        Integer zeroUnread = conversationService.getTotalUnreadCount(999L);
        assertEquals(0, zeroUnread);
    }

    /**
     * 测试会话权限验证
     * Requirements: 18.5
     */
    @Test
    public void testValidateConversationAccess() {
        String conversationId = "private_100_200";
        createTestConversation(conversationId, 100L);

        // 测试有权限的用户
        assertDoesNotThrow(() -> {
            conversationService.validateConversationAccess(conversationId, 100L);
        });

        // 测试无权限的用户
        assertThrows(BusinessException.class, () -> {
            conversationService.validateConversationAccess(conversationId, 300L);
        });

        // 测试不存在的会话
        assertThrows(BusinessException.class, () -> {
            conversationService.validateConversationAccess("private_999_888", 100L);
        });
    }

    /**
     * 测试会话置顶功能
     * Requirements: 10.1, 10.2
     */
    @Test
    public void testPinConversation() {
        String conversationId = "private_100_200";
        Long userId = 100L;
        createTestConversation(conversationId, userId);

        // 测试设置置顶
        conversationService.pinConversation(conversationId, userId, true);
        Conversation conversation = conversationMapper.selectOne(
            new QueryWrapper<Conversation>()
                .eq("conversation_id", conversationId)
                .eq("user_id", userId)
        );
        assertEquals(1, conversation.getIsPinned());

        // 测试取消置顶
        conversationService.pinConversation(conversationId, userId, false);
        conversation = conversationMapper.selectOne(
            new QueryWrapper<Conversation>()
                .eq("conversation_id", conversationId)
                .eq("user_id", userId)
        );
        assertEquals(0, conversation.getIsPinned());
    }

    /**
     * 测试会话删除功能
     * Requirements: 11.1, 11.2
     */
    @Test
    public void testDeleteConversation() {
        String conversationId = "private_100_200";
        Long userId = 100L;
        createTestConversation(conversationId, userId);

        // 验证会话存在
        Conversation conversation = conversationMapper.selectOne(
            new QueryWrapper<Conversation>()
                .eq("conversation_id", conversationId)
                .eq("user_id", userId)
        );
        assertNotNull(conversation);

        // 删除会话
        conversationService.deleteConversation(conversationId, userId);

        // 验证会话已删除
        conversation = conversationMapper.selectOne(
            new QueryWrapper<Conversation>()
                .eq("conversation_id", conversationId)
                .eq("user_id", userId)
        );
        assertNull(conversation);
    }

    // 辅助方法

    private User createTestUser(Long userId, String username) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setPasswordHash("hashedpassword");
        user.setSalt("salt");
        user.setRoleId(2); // 普通用户
        user.setStatus(1); // 正常状态
        userMapper.insert(user);
        return user;
    }

    private Message createTestMessage(Long senderId, Long receiverId, String content) {
        Message message = new Message();
        message.setMessageId(System.currentTimeMillis());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(MessageType.TEXT.getCode());
        message.setContentType(ContentType.PRIVATE_MESSAGE.getCode());
        message.setIsRead(0);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    private void createTestConversation(String conversationId, Long userId) {
        createTestConversation(conversationId, userId, 0, 100L); // Use 100L as default targetId
    }

    private void createTestConversation(String conversationId, Long userId, int unreadCount) {
        createTestConversation(conversationId, userId, unreadCount, 100L); // Use 100L as default targetId
    }

    private void createTestConversation(String conversationId, Long userId, int unreadCount, Long targetId) {
        Conversation conversation = new Conversation();
        conversation.setConversationId(conversationId);
        conversation.setUserId(userId);
        
        // 根据conversationId设置会话类型
        if (conversationId.startsWith("system_")) {
            conversation.setConversationType(ConversationType.SYSTEM_NOTIFICATION.getCode());
            conversation.setTargetId(0L);
            conversation.setTargetName("系统通知");
        } else {
            conversation.setConversationType(ConversationType.PRIVATE_CHAT.getCode());
            conversation.setTargetId(targetId);
            conversation.setTargetName("Test User");
        }
        
        conversation.setUnreadCount(unreadCount);
        conversation.setTotalCount(unreadCount);
        conversation.setIsPinned(0);
        conversationMapper.insertConversation(conversation);
    }
}