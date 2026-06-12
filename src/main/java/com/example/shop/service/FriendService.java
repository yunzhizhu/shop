package com.example.shop.service;

import com.example.shop.dto.*;

import java.util.List;

/**
 * 好友服务接口
 */
public interface FriendService {

    /**
     * 发送好友请求
     */
    void sendFriendRequest(Long fromUserId, SendFriendRequestDTO dto);

    /**
     * 处理好友请求
     */
    void handleFriendRequest(Long userId, HandleFriendRequestDTO dto);

    /**
     * 获取收到的好友请求列表
     */
    List<FriendRequestResponse> getReceivedRequests(Long userId);

    /**
     * 获取发送的好友请求列表
     */
    List<FriendRequestResponse> getSentRequests(Long userId);

    /**
     * 获取好友列表
     */
    List<FriendInfoResponse> getFriendList(Long userId);

    /**
     * 删除好友
     */
    void deleteFriend(Long userId, Long friendId);

    /**
     * 拉黑好友
     */
    void blockFriend(Long userId, Long friendId);

    /**
     * 取消拉黑
     */
    void unblockFriend(Long userId, Long friendId);

    /**
     * 更新好友备注
     */
    void updateFriendRemark(Long userId, UpdateFriendRemarkDTO dto);

    /**
     * 搜索用户
     */
    List<UserSearchResponse> searchUsers(Long currentUserId, String keyword);

    /**
     * 获取好友数量
     */
    Integer getFriendCount(Long userId);

    /**
     * 获取待处理的好友请求数量
     */
    Integer getPendingRequestCount(Long userId);
}
