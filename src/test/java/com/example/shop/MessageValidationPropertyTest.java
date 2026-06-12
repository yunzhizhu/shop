package com.example.shop;

import com.example.shop.config.TestSecurityConfig;
import com.example.shop.dto.SendMessageRequest;
import com.example.shop.dto.SystemNotificationRequest;
import com.example.shop.dto.BatchNotificationRequest;
import com.example.shop.entity.User;
import com.example.shop.exception.*;
import com.example.shop.mapper.UserMapper;
import com.example.shop.service.MessageValidationService;
import com.example.shop.service.ConversationService;
import com.example.shop.utils.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

/**
 * Property-based tests for message validation system
 * 
 * Property 13: 权限验证一致性 (Requirements 7.4, 9.4, 10.4, 11.4)
 * Property 14: 资源存在性验证 (Requirements 7.3, 9.3, 10.3, 11.3, 16.2)
 * Property 15: 输入验证错误处理 (Requirements 8.3, 8.4, 8.5, 13.3, 13.4)
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class MessageValidationPropertyTest {

    @Autowired
    private MessageValidationService messageValidationService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private ConversationService conversationService;

    private final Random random = new Random();

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        Mockito.reset(userMapper, conversationService);
    }

    /**
     * Property 13: 权限验证一致性
     * Tests that permission validation is consistent across all operations
     * Requirements: 7.4, 9.4, 10.4, 11.4
     */
    @RepeatedTest(10)
    void testPermissionValidationConsistency() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
            // Generate random user IDs
            Long currentUserId = generateRandomUserId();
            Long otherUserId = generateRandomUserId();
            
            // Ensure they are different
            while (Objects.equals(currentUserId, otherUserId)) {
                otherUserId = generateRandomUserId();
            }
            
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(currentUserId);
            
            // Test conversation access validation with proper mocking
            String validConversationId = generateConversationId(currentUserId, otherUserId);
            String invalidConversationId = generateConversationId(otherUserId, generateRandomUserId());
            
            // Mock valid conversation access
            doNothing().when(conversationService).validateConversationAccess(eq(validConversationId), eq(currentUserId));
            
            // Mock invalid conversation access
            doThrow(new MessageNotFoundException(invalidConversationId, currentUserId))
                .when(conversationService).validateConversationAccess(eq(invalidConversationId), eq(currentUserId));
            
            // Property: User should have access to their own conversations
            assertDoesNotThrow(() -> 
                messageValidationService.validateConversationAccess(validConversationId, currentUserId),
                "User should have access to their own conversation: " + validConversationId
            );
            
            // Property: User should NOT have access to other users' conversations
            assertThrows(Exception.class, () ->
                messageValidationService.validateConversationAccess(invalidConversationId, currentUserId),
                "User should not have access to unauthorized conversation: " + invalidConversationId
            );
        }
    }

    /**
     * Property 14: 资源存在性验证
     * Tests that resource existence validation works correctly
     * Requirements: 7.3, 9.3, 10.3, 11.3, 16.2
     */
    @RepeatedTest(10)
    void testResourceExistenceValidation() {
        // Generate random user IDs
        Long existingUserId = generateRandomUserId();
        Long nonExistentUserId = generateRandomUserId();
        
        // Ensure they are different
        while (Objects.equals(existingUserId, nonExistentUserId)) {
            nonExistentUserId = generateRandomUserId();
        }
        
        // Mock existing user
        User existingUser = new User();
        existingUser.setUserId(existingUserId);
        existingUser.setUsername("user_" + existingUserId);
        
        when(userMapper.selectById(existingUserId)).thenReturn(existingUser);
        when(userMapper.selectById(nonExistentUserId)).thenReturn(null);
        
        // Property: Validation should pass for existing users
        assertTrue(messageValidationService.validateUserExists(existingUserId),
            "Validation should pass for existing user: " + existingUserId
        );
        
        // Property: Validation should fail for non-existent users
        assertFalse(messageValidationService.validateUserExists(nonExistentUserId),
            "Validation should fail for non-existent user: " + nonExistentUserId
        );
        
        // Property: Null user ID should always fail
        assertFalse(messageValidationService.validateUserExists(null),
            "Validation should fail for null user ID"
        );
        
        // Property: Zero or negative user IDs should fail
        Long[] invalidIds = {0L, -1L, -generateRandomUserId()};
        for (Long invalidId : invalidIds) {
            when(userMapper.selectById(invalidId)).thenReturn(null);
            assertFalse(messageValidationService.validateUserExists(invalidId),
                "Validation should fail for invalid user ID: " + invalidId
            );
        }
    }

    /**
     * Property 15: 输入验证错误处理
     * Tests that input validation handles errors correctly
     * Requirements: 8.3, 8.4, 8.5, 13.3, 13.4
     */
    @RepeatedTest(10)
    void testInputValidationErrorHandling() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
            Long currentUserId = generateRandomUserId();
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(currentUserId);
            
            // Test core validation properties
            testSelfMessageValidation(currentUserId);
            testContentValidation();
            testPaginationValidation();
            testSearchKeywordValidation();
        }
    }

    private void testSelfMessageValidation(Long currentUserId) {
        SendMessageRequest request = new SendMessageRequest();
        
        // Property: Self-message should always be rejected
        request.setReceiverId(currentUserId);
        request.setContent("Valid content");
        request.setMessageType(1); // TEXT message type
        
        assertThrows(SelfMessageException.class, () ->
            messageValidationService.validateSendMessageRequest(request, currentUserId),
            "Self-message should be rejected for user: " + currentUserId
        );
    }

    private void testContentValidation() {
        // Property: Empty content should be rejected by content validation
        String[] emptyContents = {null, "", "   ", "\t\n  ", "    "};
        
        for (String emptyContent : emptyContents) {
            assertThrows(EmptyMessageContentException.class, () ->
                messageValidationService.validateMessageContent(emptyContent),
                "Empty content should be rejected: '" + emptyContent + "'"
            );
        }
        
        // Property: Valid content should pass validation
        String validContent = "Valid message content " + random.nextInt(1000);
        assertDoesNotThrow(() ->
            messageValidationService.validateMessageContent(validContent),
            "Valid content should pass validation: '" + validContent + "'"
        );
    }

    private void testPaginationValidation() {
        // Property: Valid pagination parameters should pass
        int[] validPages = {1, 2, 5, 10, 100};
        int[] validSizes = {1, 5, 10, 20, 50, 100};
        
        for (int page : validPages) {
            for (int size : validSizes) {
                assertDoesNotThrow(() ->
                    messageValidationService.validatePaginationParams(page, size),
                    "Valid pagination should pass: page=" + page + ", size=" + size
                );
            }
        }
        
        // Property: Invalid pagination parameters should fail
        int[] invalidPages = {0, -1, -5, -100};
        int[] invalidSizes = {0, -1, -5, -100};
        
        for (int invalidPage : invalidPages) {
            assertThrows(IllegalArgumentException.class, () ->
                messageValidationService.validatePaginationParams(invalidPage, 10),
                "Invalid page should be rejected: " + invalidPage
            );
        }
        
        for (int invalidSize : invalidSizes) {
            assertThrows(IllegalArgumentException.class, () ->
                messageValidationService.validatePaginationParams(1, invalidSize),
                "Invalid size should be rejected: " + invalidSize
            );
        }
    }

    private void testSearchKeywordValidation() {
        // Property: Valid keywords should pass
        String[] validKeywords = {
            "user", "test", "hello world", "用户", "测试", "a", "1", "user123", "test_user"
        };
        
        for (String keyword : validKeywords) {
            assertDoesNotThrow(() ->
                messageValidationService.validateSearchKeyword(keyword),
                "Valid search keyword should pass: '" + keyword + "'"
            );
        }
        
        // Property: Invalid keywords should fail
        String[] invalidKeywords = {null, "", "   ", "\t\n  ", "    "};
        
        for (String invalidKeyword : invalidKeywords) {
            assertThrows(IllegalArgumentException.class, () ->
                messageValidationService.validateSearchKeyword(invalidKeyword),
                "Invalid search keyword should be rejected: '" + invalidKeyword + "'"
            );
        }
    }

    // Helper methods
    private Long generateRandomUserId() {
        return ThreadLocalRandom.current().nextLong(1, 10000);
    }

    private String generateConversationId(Long userId1, Long userId2) {
        Long smallerId = Math.min(userId1, userId2);
        Long largerId = Math.max(userId1, userId2);
        return "private_" + smallerId + "_" + largerId;
    }
}