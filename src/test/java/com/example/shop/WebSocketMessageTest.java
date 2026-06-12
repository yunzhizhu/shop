package com.example.shop;

import com.example.shop.config.TestSecurityConfig;
import com.example.shop.dto.SendMessageRequest;
import com.example.shop.dto.UnreadUpdateDTO;
import com.example.shop.dto.WebSocketMessageDTO;
import com.example.shop.entity.User;
import com.example.shop.enums.ContentType;
import com.example.shop.enums.MessageType;
import com.example.shop.mapper.UserMapper;
import com.example.shop.service.MessageService;
import com.example.shop.service.WebSocketService;
import com.example.shop.utils.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebSocket消息推送测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
public class WebSocketMessageTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserMapper userMapper;

    @MockBean
    private WebSocketService webSocketService;

    private User testSender;
    private User testReceiver;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testSender = new User();
        testSender.setUserId(2001L);
        testSender.setUsername("websocket_sender");
        testSender.setPasswordHash("hash");
        testSender.setSalt("salt");
        testSender.setRoleId(2);
        testSender.setStatus(1);
        userMapper.insert(testSender);

        testReceiver = new User();
        testReceiver.setUserId(2002L);
        testReceiver.setUsername("websocket_receiver");
        testReceiver.setPasswordHash("hash");
        testReceiver.setSalt("salt");
        testReceiver.setRoleId(2);
        testReceiver.setStatus(1);
        userMapper.insert(testReceiver);
    }

    @Test
    void testWebSocketMessagePushingOnSendMessage() {
        // Mock当前用户
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(testSender.getUserId());

            // 创建发送消息请求
            SendMessageRequest request = new SendMessageRequest();
            request.setReceiverId(testReceiver.getUserId());
            request.setContent("WebSocket测试消息");
            request.setMessageType(MessageType.TEXT.getCode());
            request.setContentType(ContentType.PRIVATE_MESSAGE.getCode());

            // 发送消息
            Long messageId = messageService.sendMessage(request);

            // 验证消息发送成功
            assertNotNull(messageId);

            // 验证WebSocket推送被调用
            verify(webSocketService, times(1)).pushMessage(eq(testReceiver.getUserId()), any(WebSocketMessageDTO.class));
            verify(webSocketService, times(1)).pushUnreadUpdate(eq(testReceiver.getUserId()), any(UnreadUpdateDTO.class));
        }
    }

    @Test
    void testWebSocketMessageDTOStructure() {
        // Mock当前用户
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(testSender.getUserId());

            // 创建发送消息请求
            SendMessageRequest request = new SendMessageRequest();
            request.setReceiverId(testReceiver.getUserId());
            request.setContent("测试WebSocket消息结构");
            request.setMessageType(MessageType.TEXT.getCode());
            request.setContentType(ContentType.PRIVATE_MESSAGE.getCode());

            // 发送消息
            messageService.sendMessage(request);

            // 验证WebSocket消息DTO结构
            verify(webSocketService).pushMessage(eq(testReceiver.getUserId()), argThat(dto -> {
                assertNotNull(dto.getMessageId());
                assertNotNull(dto.getConversationId());
                assertEquals(testSender.getUserId(), dto.getSenderId());
                assertEquals(testReceiver.getUserId(), dto.getReceiverId());
                assertEquals("测试WebSocket消息结构", dto.getContent());
                assertEquals(MessageType.TEXT.getCode(), dto.getMessageType());
                assertEquals(ContentType.PRIVATE_MESSAGE.getCode(), dto.getContentType());
                assertEquals(testSender.getUsername(), dto.getSenderName());
                assertEquals(false, dto.getIsSystem());
                assertTrue(dto.getConversationId().startsWith("private_"));
                return true;
            }));
        }
    }

    @Test
    void testUnreadUpdateDTOStructure() {
        // Mock当前用户
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(testSender.getUserId());

            // 创建发送消息请求
            SendMessageRequest request = new SendMessageRequest();
            request.setReceiverId(testReceiver.getUserId());
            request.setContent("测试未读数更新");
            request.setMessageType(MessageType.TEXT.getCode());
            request.setContentType(ContentType.PRIVATE_MESSAGE.getCode());

            // 发送消息
            messageService.sendMessage(request);

            // 验证未读数更新DTO结构
            verify(webSocketService).pushUnreadUpdate(eq(testReceiver.getUserId()), argThat(dto -> {
                assertNotNull(dto.getConversationId());
                assertTrue(dto.getConversationId().startsWith("private_"));
                assertNotNull(dto.getUnreadCount());
                assertNotNull(dto.getTotalUnreadCount());
                assertEquals("unread", dto.getPushType());
                return true;
            }));
        }
    }
}