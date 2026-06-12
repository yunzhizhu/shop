package com.example.shop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.dto.*;
import com.example.shop.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员消息控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/message")
public class AdminMessageController {

    @Autowired
    private MessageService messageService;

    // ========== 新的管理员消息功能 ==========

    /**
     * 发送系统通知给指定用户
     */
    @PostMapping("/system-notification/send")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "发送系统通知", module = "消息模块", action = "sendSystemNotification")
    public Result<Map<String, Object>> sendSystemNotification(@Valid @RequestBody SystemNotificationRequest request) {
        Long messageId = messageService.sendSystemNotification(request);
        
        Map<String, Object> data = new HashMap<>();
        data.put("messageId", messageId);
        
        return Result.success("发送成功", data);
    }

    /**
     * 批量发送系统通知
     */
    @PostMapping("/system-notification/batch-send")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "批量发送系统通知", module = "消息模块", action = "batchSendNotification")
    public Result<BatchSendResult> batchSendNotification(@Valid @RequestBody BatchNotificationRequest request) {
        BatchSendResult result = messageService.batchSendNotification(request);
        return Result.success("批量发送完成", result);
    }

    /**
     * 获取系统通知列表（管理员）
     */
    @GetMapping("/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<MessageHistoryResponse>> getNotifications(
            @RequestParam(required = false) Integer contentType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        IPage<MessageHistoryResponse> notificationPage = messageService.getAdminNotifications(contentType, page, size);
        return Result.success(notificationPage);
    }

}
