package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.entity.Conversation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会话Mapper接口
 */
public interface ConversationMapper extends BaseMapper<Conversation> {

    /**
     * 根据用户ID和目标ID查询会话
     */
    Conversation selectByUserAndTarget(@Param("userId") Long userId, 
                                     @Param("targetId") Long targetId, 
                                     @Param("conversationType") Integer conversationType);

    /**
     * 分页查询用户会话列表（按置顶和时间排序）
     */
    IPage<Conversation> selectConversationPage(Page<Conversation> page, 
                                             @Param("userId") Long userId);

    /**
     * 搜索用户会话
     */
    IPage<Conversation> searchConversations(Page<Conversation> page, 
                                          @Param("userId") Long userId, 
                                          @Param("keyword") String keyword);

    /**
     * 获取用户总未读数
     */
    Integer selectTotalUnreadCount(@Param("userId") Long userId);

    /**
     * 获取用户私信未读数
     */
    Integer selectPrivateUnreadCount(@Param("userId") Long userId);

    /**
     * 获取用户系统通知未读数
     */
    Integer selectSystemUnreadCount(@Param("userId") Long userId);

    /**
     * 获取指定会话的未读数
     */
    Integer selectConversationUnreadCount(@Param("conversationId") String conversationId, 
                                        @Param("userId") Long userId);

    /**
     * 更新会话未读数
     */
    int updateUnreadCount(@Param("conversationId") String conversationId, 
                         @Param("userId") Long userId, 
                         @Param("delta") Integer delta);

    /**
     * 重置会话未读数
     */
    int resetUnreadCount(@Param("conversationId") String conversationId, 
                        @Param("userId") Long userId);

    /**
     * 更新会话置顶状态
     */
    int updatePinnedStatus(@Param("conversationId") String conversationId, 
                          @Param("userId") Long userId, 
                          @Param("isPinned") Integer isPinned);

    /**
     * 软删除会话（设置删除标记）
     */
    int softDeleteConversation(@Param("conversationId") String conversationId, 
                              @Param("userId") Long userId);

    /**
     * 批量获取会话信息
     */
    List<Conversation> selectByConversationIds(@Param("conversationIds") List<String> conversationIds, 
                                             @Param("userId") Long userId);

    /**
     * 插入会话记录（处理复合主键）
     */
    int insertConversation(Conversation conversation);

    /**
     * 更新会话记录（处理复合主键）
     */
    int updateConversation(Conversation conversation);
}