package com.example.shop;

import com.example.shop.config.TestSecurityConfig;
import com.example.shop.dto.*;
import com.example.shop.entity.User;
import com.example.shop.enums.MessageType;
import com.example.shop.mapper.UserMapper;
import com.example.shop.service.MessageService;
import com.example.shop.utils.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 消息服务重构测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
public class MessageServiceRefactorTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserMapper userMapper;

    private User testSender;
    private User testReceiver;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testSender = new User();
        testSender.setUserId(1001L);
        testSender.setUsername("test_sender");
        testSender.setPasswordHash("hash");
        testSender.setSalt("salt");
        testSender.setRoleId(2);
        testSender.setStatus(1);
        userMapper.insert(testSender);

        testReceiver = new User();
        testReceiver.setUserId(1002L);
        testReceiver.setUsername("test_receiver");
        testReceiver.setPasswordHash("hash");
        testReceiver.setSalt("salt");
        testReceiver.setRoleId(2);
        testReceiver.setStatus(1);
        userMapper.insert(testReceiver);
    }

    @Test
    void testSendMessage() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(testSender.getUserId());

            SendMessageRequest request = new SendMessageRequest();
            request.setReceiverId(testReceiver.getUserId());
            request.setContent("Test message");
            request.setMessageType(MessageType.TEXT.getCode());

            Long messageId = messageService.sendMessage(request);
            assertNotNull(messageId);
            assertTrue(messageId > 0);
        }
    }

    @Test
    void testGetTotalUnreadCount() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(testSender.getUserId());

            TotalUnreadResponse response = messageService.getTotalUnreadCount();
            assertNotNull(response);
            assertNotNull(response.getTotalUnreadCount());
            assertTrue(response.getTotalUnreadCount() >= 0);
        }
    }

    @Test
    void testGetConversationList() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(testSender.getUserId());

            var response = messageService.getConversationList(1, 10);
            assertNotNull(response);
            assertNotNull(response.getRecords());
        }
    }

    @Test
    void testSendSystemNotification() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(1L); // Admin user

            SystemNotificationRequest request = new SystemNotificationRequest();
            request.setReceiverId(testReceiver.getUserId());
            request.setContent("System notification test");
            request.setContentType(2); // System notification
            request.setMessageType(MessageType.TEXT.getCode());

            Long messageId = messageService.sendSystemNotification(request);
            assertNotNull(messageId);
            assertTrue(messageId > 0);
        }
    }

    @Test
    void testBatchSendNotification() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(1L); // Admin user

            BatchNotificationRequest request = new BatchNotificationRequest();
            request.setContent("Batch notification test");
            request.setContentType(2); // System notification
            request.setMessageType(MessageType.TEXT.getCode());
            // userIds is null, should send to all active users

            BatchSendResult result = messageService.batchSendNotification(request);
            assertNotNull(result);
            assertNotNull(result.getSuccessCount());
            assertNotNull(result.getFailedCount());
            assertNotNull(result.getTotalCount());
        }
    }
}