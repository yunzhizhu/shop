package com.example.shop;

import com.example.shop.mapper.MessageMapper;
import com.example.shop.mapper.ConversationMapper;
import com.example.shop.service.MessageService;
import com.example.shop.service.ConversationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MessageTest {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ConversationService conversationService;

    @Test
    public void testMapperLoading() {
        // 简单测试Mapper是否能正常加载
        System.out.println("MessageMapper loaded: " + (messageMapper != null));
        System.out.println("ConversationMapper loaded: " + (conversationMapper != null));
        System.out.println("MessageService loaded: " + (messageService != null));
        System.out.println("ConversationService loaded: " + (conversationService != null));
    }
}
