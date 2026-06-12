package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shop.dto.FriendRequestResponse;
import com.example.shop.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 好友请求Mapper
 */
@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {

    /**
     * 获取收到的好友请求列表
     */
    List<FriendRequestResponse> selectReceivedRequests(@Param("userId") Long userId);

    /**
     * 获取发送的好友请求列表
     */
    List<FriendRequestResponse> selectSentRequests(@Param("userId") Long userId);

    /**
     * 检查是否有待处理的请求
     */
    Integer checkPendingRequest(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);
}
