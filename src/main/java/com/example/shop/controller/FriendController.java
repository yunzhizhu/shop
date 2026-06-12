package com.example.shop.controller;

import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.dto.*;
import com.example.shop.service.FriendService;
import com.example.shop.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 好友管理控制器
 */
@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /**
     * 发送好友请求
     */
    @PostMapping("/request/send")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "发送好友请求", module = "Friend", action = "SEND_REQUEST")
    public Result<Void> sendFriendRequest(@Valid @RequestBody SendFriendRequestDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        friendService.sendFriendRequest(userId, dto);
        return Result.success();
    }

    /**
     * 处理好友请求
     */
    @PostMapping("/request/handle")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "处理好友请求", module = "Friend", action = "HANDLE_REQUEST")
    public Result<Void> handleFriendRequest(@Valid @RequestBody HandleFriendRequestDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        friendService.handleFriendRequest(userId, dto);
        return Result.success();
    }

    /**
     * 获取收到的好友请求列表
     */
    @GetMapping("/request/received")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<FriendRequestResponse>> getReceivedRequests() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<FriendRequestResponse> requests = friendService.getReceivedRequests(userId);
        return Result.success(requests);
    }

    /**
     * 获取发送的好友请求列表
     */
    @GetMapping("/request/sent")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<FriendRequestResponse>> getSentRequests() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<FriendRequestResponse> requests = friendService.getSentRequests(userId);
        return Result.success(requests);
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<FriendInfoResponse>> getFriendList() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<FriendInfoResponse> friends = friendService.getFriendList(userId);
        return Result.success(friends);
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "删除好友", module = "Friend", action = "DELETE")
    public Result<Void> deleteFriend(@PathVariable Long friendId) {
        Long userId = SecurityUtil.getCurrentUserId();
        friendService.deleteFriend(userId, friendId);
        return Result.success();
    }

    /**
     * 拉黑好友
     */
    @PostMapping("/block/{friendId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "拉黑好友", module = "Friend", action = "BLOCK")
    public Result<Void> blockFriend(@PathVariable Long friendId) {
        Long userId = SecurityUtil.getCurrentUserId();
        friendService.blockFriend(userId, friendId);
        return Result.success();
    }

    /**
     * 取消拉黑
     */
    @PostMapping("/unblock/{friendId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "取消拉黑", module = "Friend", action = "UNBLOCK")
    public Result<Void> unblockFriend(@PathVariable Long friendId) {
        Long userId = SecurityUtil.getCurrentUserId();
        friendService.unblockFriend(userId, friendId);
        return Result.success();
    }

    /**
     * 更新好友备注
     */
    @PutMapping("/remark")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "更新好友备注", module = "Friend", action = "UPDATE_REMARK")
    public Result<Void> updateFriendRemark(@Valid @RequestBody UpdateFriendRemarkDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        friendService.updateFriendRemark(userId, dto);
        return Result.success();
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<UserSearchResponse>> searchUsers(@RequestParam String keyword) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<UserSearchResponse> users = friendService.searchUsers(userId, keyword);
        return Result.success(users);
    }

    /**
     * 获取好友数量
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<Integer> getFriendCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        Integer count = friendService.getFriendCount(userId);
        return Result.success(count);
    }
}
