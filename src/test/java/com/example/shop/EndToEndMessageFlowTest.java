package com.example.shop;

import com.example.shop.config.TestSecurityConfig;
import com.example.shop.dto.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 端到端消息流程测试
 * 测试完整的用户消息交互流程和管理员系统通知流程
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
public class EndToEndMessageFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCompleteUserMessageInteractionFlow() throws Exception {
        Long senderId = 1L;
        Long receiverId = 2L;

        // === 第一阶段：发送消息 ===
        SendMessageRequest sendRequest = new SendMessageRequest();
        sendRequest.setReceiverId(receiverId);
        sendRequest.setContent("Hello! How are you today?");
        sendRequest.setContentType(1); // 1-普通私信
        sendRequest.setMessageType(1); // 1-文本消息

        mockMvc.perform(post("/messages/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isOk());

        // === 第二阶段：发送者查看会话列表 ===
        mockMvc.perform(get("/messages/conversations")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk());

        // === 第三阶段：接收者查看未读消息 ===
        mockMvc.perform(get("/messages/unread/total"))
                .andExpect(status().isOk());

        // === 第四阶段：接收者回复消息 ===
        SendMessageRequest replyRequest = new SendMessageRequest();
        replyRequest.setReceiverId(senderId);
        replyRequest.setContent("I'm doing great, thanks for asking!");
        replyRequest.setContentType(1); // 1-普通私信
        replyRequest.setMessageType(1); // 1-文本消息

        mockMvc.perform(post("/messages/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(replyRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testCompleteAdminSystemNotificationFlow() throws Exception {
        Long targetUserId = 1L;

        // === 第一阶段：管理员发送系统通知 ===
        String systemRequestBody = """
            {
                "receiverId": 1,
                "content": "The system will be under maintenance from 2:00 AM to 4:00 AM tomorrow.",
                "contentType": 2,
                "messageType": 1
            }
            """;

        mockMvc.perform(post("/admin/messages/system-notification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(systemRequestBody))
                .andExpect(status().isOk());

        // === 第二阶段：管理员发送批量通知 ===
        String batchRequestBody = """
            {
                "userIds": [1, 2, 3],
                "content": "Please update your profile information by the end of this week.",
                "contentType": 2,
                "messageType": 1
            }
            """;

        mockMvc.perform(post("/admin/messages/batch-notification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchRequestBody))
                .andExpect(status().isOk());

        // === 第三阶段：管理员查看消息统计 ===
        mockMvc.perform(get("/admin/messages/statistics")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    void testConversationLifecycleFlow() throws Exception {
        Long receiverId = 2L;

        // === 第一阶段：创建会话（通过发送消息） ===
        SendMessageRequest createRequest = new SendMessageRequest();
        createRequest.setReceiverId(receiverId);
        createRequest.setContent("Starting a new conversation");
        createRequest.setContentType(1); // 1-普通私信
        createRequest.setMessageType(1); // 1-文本消息

        mockMvc.perform(post("/messages/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        // === 第二阶段：会话搜索 ===
        mockMvc.perform(get("/messages/conversations/search")
                .param("keyword", "conversation")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }
}