package com.example.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shop.dto.*;
import com.example.shop.entity.FriendRelation;
import com.example.shop.entity.FriendRequest;
import com.example.shop.entity.User;
import com.example.shop.enums.FriendRequestStatus;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.FriendRelationMapper;
import com.example.shop.mapper.FriendRequestMapper;
import com.example.shop.mapper.UserMapper;
import com.example.shop.service.FriendService;
import com.example.shop.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 好友服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendRelationMapper friendRelationMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final UserMapper userMapper;
    private final com.example.shop.helper.FileUploadHelper fileUploadHelper;
    private final WebSocketService webSocketService;

    private static final Integer MAX_FRIENDS = 500;
    private static final Integer REJECT_COOLDOWN_HOURS = 24;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendFriendRequest(Long fromUserId, SendFriendRequestDTO dto) {
        Long toUserId = dto.getToUserId();

        // 验证：不能给自己发送请求
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException("不能添加自己为好友");
        }

        // 验证：目标用户是否存在
        User toUser = userMapper.selectById(toUserId);
        if (toUser == null) {
            throw new BusinessException("目标用户不存在");
        }

        // 验证：是否已经是好友
        Integer isFriend = friendRelationMapper.checkIsFriend(fromUserId, toUserId);
        if (isFriend > 0) {
            throw new BusinessException("已经是好友关系");
        }

        // 验证：是否有待处理的请求
        Integer hasPending = friendRequestMapper.checkPendingRequest(fromUserId, toUserId);
        if (hasPending > 0) {
            throw new BusinessException("已有待处理的好友请求");
        }

        // 验证：检查是否在拒绝冷却期内
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getFromUserId, fromUserId)
                .eq(FriendRequest::getToUserId, toUserId)
                .eq(FriendRequest::getStatus, FriendRequestStatus.REJECTED.getCode())
                .orderByDesc(FriendRequest::getHandledAt)
                .last("LIMIT 1");
        FriendRequest lastRejected = friendRequestMapper.selectOne(wrapper);
        
        if (lastRejected != null && lastRejected.getHandledAt() != null) {
            LocalDateTime cooldownEnd = lastRejected.getHandledAt().plusHours(REJECT_COOLDOWN_HOURS);
            if (LocalDateTime.now().isBefore(cooldownEnd)) {
                throw new BusinessException("请求被拒绝后24小时内不能重复发送");
            }
        }

        // 验证：好友数量限制
        Integer friendCount = friendRelationMapper.countFriends(fromUserId);
        if (friendCount >= MAX_FRIENDS) {
            throw new BusinessException("好友数量已达上限（" + MAX_FRIENDS + "个）");
        }

        // 创建好友请求
        FriendRequest request = new FriendRequest();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setMessage(dto.getMessage());
        request.setStatus(FriendRequestStatus.PENDING.getCode());
        friendRequestMapper.insert(request);
        
        // 推送WebSocket通知给接收者
        try {
            webSocketService.pushFriendRequestNotification(toUserId);
        } catch (Exception e) {
            log.error("推送好友请求通知失败: toUserId={}", toUserId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFriendRequest(Long userId, HandleFriendRequestDTO dto) {
        // 查询请求
        FriendRequest request = friendRequestMapper.selectById(dto.getRequestId());
        if (request == null) {
            throw new BusinessException("好友请求不存在");
        }

        // 验证：只能处理发给自己的请求
        if (!request.getToUserId().equals(userId)) {
            throw new BusinessException("无权处理此请求");
        }

        // 验证：请求状态
        if (!request.getStatus().equals(FriendRequestStatus.PENDING.getCode())) {
            throw new BusinessException("请求已被处理");
        }

        if (dto.getAccept()) {
            // 接受请求
            // 验证：好友数量限制
            Integer friendCount = friendRelationMapper.countFriends(userId);
            if (friendCount >= MAX_FRIENDS) {
                throw new BusinessException("好友数量已达上限（" + MAX_FRIENDS + "个）");
            }

            // 检查是否已经是好友关系（避免重复创建）
            Integer existingRelation = friendRelationMapper.checkIsFriend(userId, request.getFromUserId());
            if (existingRelation == 0) {
                // 创建双向好友关系
                FriendRelation relation1 = new FriendRelation();
                relation1.setUserId(userId);
                relation1.setFriendId(request.getFromUserId());
                relation1.setIsBlocked(0);
                friendRelationMapper.insert(relation1);

                FriendRelation relation2 = new FriendRelation();
                relation2.setUserId(request.getFromUserId());
                relation2.setFriendId(userId);
                relation2.setIsBlocked(0);
                friendRelationMapper.insert(relation2);
            }

            // 更新当前请求状态
            request.setStatus(FriendRequestStatus.ACCEPTED.getCode());
            request.setHandledAt(LocalDateTime.now());
            friendRequestMapper.updateById(request);

            // 处理交叉好友请求：查找并自动接受反向请求
            handleCrossRequest(request.getFromUserId(), userId);
        } else {
            // 拒绝请求
            request.setStatus(FriendRequestStatus.REJECTED.getCode());
            request.setHandledAt(LocalDateTime.now());
            friendRequestMapper.updateById(request);
        }
    }

    /**
     * 处理交叉好友请求
     * 当用户A接受用户B的请求后，自动处理用户A发给用户B的待处理请求
     */
    private void handleCrossRequest(Long fromUserId, Long toUserId) {
        // 查找反向的待处理请求
        // fromUserId是原请求的发送者，toUserId是原请求的接收者（当前处理请求的用户）
        // 我们要查找的是：toUserId发给fromUserId的待处理请求
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getFromUserId, toUserId)
                .eq(FriendRequest::getToUserId, fromUserId)
                .eq(FriendRequest::getStatus, FriendRequestStatus.PENDING.getCode());
        
        FriendRequest crossRequest = friendRequestMapper.selectOne(wrapper);
        
        if (crossRequest != null) {
            // 自动接受反向请求
            crossRequest.setStatus(FriendRequestStatus.ACCEPTED.getCode());
            crossRequest.setHandledAt(LocalDateTime.now());
            friendRequestMapper.updateById(crossRequest);
        }
    }

    @Override
    public List<FriendRequestResponse> getReceivedRequests(Long userId) {
        List<FriendRequestResponse> requests = friendRequestMapper.selectReceivedRequests(userId);
        
        // 转换头像为完整URL
        requests.forEach(request -> {
            if (request.getFromAvatar() != null && !request.getFromAvatar().isEmpty()) {
                request.setFromAvatar(fileUploadHelper.toFullUrl(request.getFromAvatar()));
            }
            if (request.getToAvatar() != null && !request.getToAvatar().isEmpty()) {
                request.setToAvatar(fileUploadHelper.toFullUrl(request.getToAvatar()));
            }
        });
        
        return requests;
    }

    @Override
    public List<FriendRequestResponse> getSentRequests(Long userId) {
        List<FriendRequestResponse> requests = friendRequestMapper.selectSentRequests(userId);
        
        // 转换头像为完整URL
        requests.forEach(request -> {
            if (request.getFromAvatar() != null && !request.getFromAvatar().isEmpty()) {
                request.setFromAvatar(fileUploadHelper.toFullUrl(request.getFromAvatar()));
            }
            if (request.getToAvatar() != null && !request.getToAvatar().isEmpty()) {
                request.setToAvatar(fileUploadHelper.toFullUrl(request.getToAvatar()));
            }
        });
        
        return requests;
    }

    @Override
    public List<FriendInfoResponse> getFriendList(Long userId) {
        List<FriendInfoResponse> friendList = friendRelationMapper.selectFriendList(userId);
        
        // 处理NULL字段，确保API响应与文档一致
        for (FriendInfoResponse friend : friendList) {
            // 转换头像为完整URL
            if (friend.getAvatar() != null && !friend.getAvatar().isEmpty()) {
                friend.setAvatar(fileUploadHelper.toFullUrl(friend.getAvatar()));
            } else {
                friend.setAvatar("");
            }
            if (friend.getPhone() == null) {
                friend.setPhone("");
            }
            if (friend.getRemark() == null) {
                friend.setRemark("");
            }
        }
        
        return friendList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(Long userId, Long friendId) {
        // 验证：是否是好友
        Integer isFriend = friendRelationMapper.checkIsFriend(userId, friendId);
        if (isFriend == 0) {
            throw new BusinessException("不是好友关系");
        }

        // 删除双向好友关系
        LambdaQueryWrapper<FriendRelation> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(FriendRelation::getUserId, userId)
                .eq(FriendRelation::getFriendId, friendId);
        friendRelationMapper.delete(wrapper1);

        LambdaQueryWrapper<FriendRelation> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(FriendRelation::getUserId, friendId)
                .eq(FriendRelation::getFriendId, userId);
        friendRelationMapper.delete(wrapper2);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void blockFriend(Long userId, Long friendId) {
        // 查询好友关系
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRelation::getUserId, userId)
                .eq(FriendRelation::getFriendId, friendId);
        FriendRelation relation = friendRelationMapper.selectOne(wrapper);

        if (relation == null) {
            throw new BusinessException("不是好友关系");
        }

        // 更新拉黑状态
        relation.setIsBlocked(1);
        friendRelationMapper.updateById(relation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unblockFriend(Long userId, Long friendId) {
        // 查询好友关系
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRelation::getUserId, userId)
                .eq(FriendRelation::getFriendId, friendId);
        FriendRelation relation = friendRelationMapper.selectOne(wrapper);

        if (relation == null) {
            throw new BusinessException("不是好友关系");
        }

        // 更新拉黑状态
        relation.setIsBlocked(0);
        friendRelationMapper.updateById(relation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFriendRemark(Long userId, UpdateFriendRemarkDTO dto) {
        // 查询好友关系
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRelation::getUserId, userId)
                .eq(FriendRelation::getFriendId, dto.getFriendId());
        FriendRelation relation = friendRelationMapper.selectOne(wrapper);

        if (relation == null) {
            throw new BusinessException("不是好友关系");
        }

        // 更新备注
        relation.setRemark(dto.getRemark());
        friendRelationMapper.updateById(relation);
    }

    @Override
    public List<UserSearchResponse> searchUsers(Long currentUserId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 搜索用户（通过用户名、手机号）
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(User::getUsername, keyword)
                .or().like(User::getPhone, keyword))
                .eq(User::getStatus, 1)
                .ne(User::getUserId, currentUserId)
                .last("LIMIT 20");
        
        List<User> users = userMapper.selectList(wrapper);

        // 查询当前用户的好友列表
        List<FriendInfoResponse> friends = friendRelationMapper.selectFriendList(currentUserId);
        List<Long> friendIds = friends.stream()
                .map(FriendInfoResponse::getUserId)
                .collect(Collectors.toList());

        // 查询待处理的请求
        List<FriendRequestResponse> sentRequests = friendRequestMapper.selectSentRequests(currentUserId);
        List<Long> pendingRequestUserIds = sentRequests.stream()
                .filter(r -> r.getStatus().equals(FriendRequestStatus.PENDING.getCode()))
                .map(FriendRequestResponse::getToUserId)
                .collect(Collectors.toList());

        // 构建响应
        return users.stream().map(user -> {
            UserSearchResponse response = new UserSearchResponse();
            response.setUserId(user.getUserId());
            response.setUsername(user.getUsername());
            // 转换头像为完整URL
            String avatar = user.getAvatar();
            if (avatar != null && !avatar.isEmpty()) {
                avatar = fileUploadHelper.toFullUrl(avatar);
            }
            response.setAvatar(avatar);
            response.setIsFriend(friendIds.contains(user.getUserId()));
            response.setHasRequestPending(pendingRequestUserIds.contains(user.getUserId()));
            
            // 手机号部分显示
            if (user.getPhone() != null && user.getPhone().length() >= 11) {
                response.setPhone(user.getPhone().substring(0, 3) + "****" + user.getPhone().substring(7));
            }
            
            return response;
        }).collect(Collectors.toList());
    }

    @Override
    public Integer getFriendCount(Long userId) {
        return friendRelationMapper.countFriends(userId);
    }

    @Override
    public Integer getPendingRequestCount(Long userId) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getToUserId, userId)
                .eq(FriendRequest::getStatus, FriendRequestStatus.PENDING.getCode());
        return Math.toIntExact(friendRequestMapper.selectCount(wrapper));
    }
}
