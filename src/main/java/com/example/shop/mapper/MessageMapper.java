package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.dto.MessageHistoryResponse;
import com.example.shop.dto.MessageListResponse;
import com.example.shop.entity.Message;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息Mapper接口
 */
public interface MessageMapper extends BaseMapper<Message> {

    // ========== 新的基于会话的查询方法 ==========

    /**
     * 根据会话ID获取消息历史记录
     */
    IPage<MessageHistoryResponse> selectMessageHistoryByConversation(Page<MessageHistoryResponse> page,
                                                                    @Param("conversationId") String conversationId);

    /**
     * 标记会话中的消息为已读
     */
    int markConversationAsRead(@Param("conversationId") String conversationId,
                              @Param("userId") Long userId);

    /**
     * 标记单条消息为已读
     */
    int markSingleMessageAsRead(@Param("messageId") Long messageId,
                               @Param("userId") Long userId);

    /**
     * 获取系统通知列表（管理员）
     */
    IPage<MessageHistoryResponse> selectAdminNotifications(Page<MessageHistoryResponse> page,
                                                          @Param("contentType") Integer contentType);

    // ========== 已有的方法（保留兼容性） ==========

    /**
     * 获取用户消息列表
     */
    List<MessageListResponse> selectMessageList(@Param("userId") Long userId);

    /**
     * 获取聊天历史记录
     */
    IPage<MessageHistoryResponse> selectMessageHistory(Page<MessageHistoryResponse> page,
                                                      @Param("userId") Long userId,
                                                      @Param("chatUserId") Long chatUserId);

    /**
     * 获取未读消息数
     */
    Integer getUnreadCount(@Param("userId") Long userId, @Param("senderId") Long senderId);

    /**
     * 标记消息为已读
     */
    int markAsRead(@Param("userId") Long userId, @Param("senderId") Long senderId);
}
