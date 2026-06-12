package com.example.shop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.dto.*;
import com.example.shop.helper.FileUploadHelper;
import com.example.shop.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 消息控制器
 */
@Slf4j
@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private FileUploadHelper fileUploadHelper;

    // ========== 新的基于会话的API接口 ==========

    /**
     * 上传消息图片
     */
    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "上传消息图片", module = "消息模块", action = "uploadMessageImage")
    public Result<Map<String, Object>> uploadMessageImage(@RequestParam(value = "file", required = false) MultipartFile file,
                                                           @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            // 支持 file 或 image 字段名
            MultipartFile uploadFile = file != null ? file : image;
            
            if (uploadFile == null || uploadFile.isEmpty()) {
                return Result.error(400, "请选择要上传的图片");
            }
            
            // 验证文件
            fileUploadHelper.validateFile(uploadFile);
            
            // 生成唯一文件名
            String originalFilename = uploadFile.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + extension;
            
            // 确保目录存在
            String uploadPath = fileUploadHelper.getMessagePath();
            fileUploadHelper.createDirectoryIfNotExists(uploadPath);
            
            // 保存文件
            Path filePath = Paths.get(uploadPath, fileName);
            Files.write(filePath, uploadFile.getBytes());
            
            // 生成相对路径URL，查询时动态拼接域名
            String imageUrl = fileUploadHelper.getMessageAccessUrl(fileName);
            
            Map<String, Object> data = new HashMap<>();
            data.put("imageUrl", imageUrl);
            
            return Result.success("上传成功", data);
        } catch (IOException e) {
            log.error("上传消息图片失败", e);
            return Result.error(500, "上传失败");
        }
    }

    /**
     * 发送私信
     */
    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "发送私信", module = "消息模块", action = "sendMessage")
    public Result<Map<String, Object>> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Long messageId = messageService.sendMessage(request);
        
        Map<String, Object> data = new HashMap<>();
        data.put("messageId", messageId);
        
        return Result.success("发送成功", data);
    }

    /**
     * 获取总未读消息数
     */
    @GetMapping("/total-unread")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<TotalUnreadResponse> getTotalUnreadCount() {
        TotalUnreadResponse response = messageService.getTotalUnreadCount();
        return Result.success(response);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<IPage<ConversationListResponse>> getConversationList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        IPage<ConversationListResponse> conversationPage = messageService.getConversationList(page, size);
        return Result.success(conversationPage);
    }

    /**
     * 获取消息历史记录（基于会话）
     */
    @GetMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<IPage<MessageHistoryResponse>> getMessageHistory(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        IPage<MessageHistoryResponse> historyPage = messageService.getMessageHistory(conversationId, page, size);
        return Result.success(historyPage);
    }

    /**
     * 标记会话为已读
     */
    @PostMapping("/conversations/mark-read")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "标记会话已读", module = "消息模块", action = "markConversationAsRead")
    public Result<Void> markConversationAsRead(@Valid @RequestBody ConversationReadRequest request) {
        messageService.markConversationAsRead(request);
        return Result.success("标记成功", null);
    }

    /**
     * 标记单条消息为已读
     */
    @PostMapping("/mark-read")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "标记单条消息已读", module = "消息模块", action = "markSingleMessageAsRead")
    public Result<Void> markSingleMessageAsRead(@RequestBody Map<String, Long> request) {
        Long messageId = request.get("messageId");
        if (messageId == null) {
            return Result.error(400, "消息ID不能为空");
        }
        messageService.markSingleMessageAsRead(messageId);
        return Result.success("标记成功", null);
    }

    /**
     * 设置会话置顶状态
     */
    @PostMapping("/conversations/pin")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "设置会话置顶", module = "消息模块", action = "pinConversation")
    public Result<Void> pinConversation(@Valid @RequestBody ConversationPinRequest request) {
        messageService.pinConversation(request);
        return Result.success("设置成功", null);
    }

    /**
     * 删除会话
     */
    @PostMapping("/conversations/delete")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "删除会话", module = "消息模块", action = "deleteConversation")
    public Result<Void> deleteConversation(@Valid @RequestBody ConversationDeleteRequest request) {
        messageService.deleteConversation(request);
        return Result.success("删除成功", null);
    }

    /**
     * 搜索会话
     */
    @GetMapping("/conversations/search")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<IPage<ConversationSearchResponse>> searchConversations(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        IPage<ConversationSearchResponse> searchPage = messageService.searchConversations(keyword, page, size);
        return Result.success(searchPage);
    }

    // ========== 已废弃的API接口（保留兼容性） ==========

    /**
     * 获取消息列表
     * @deprecated 使用 /conversations 替代
     */
    @Deprecated
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<MessageListResponse>> getMessageList() {
        List<MessageListResponse> messageList = messageService.getMessageList();
        return Result.success(messageList);
    }

    /**
     * 获取聊天历史记录
     * @deprecated 使用 /conversations/{conversationId}/messages 替代
     */
    @Deprecated
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<IPage<MessageHistoryResponse>> getChatHistory(
            @RequestParam Long chatUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        IPage<MessageHistoryResponse> historyPage = messageService.getChatHistory(chatUserId, page, size);
        return Result.success(historyPage);
    }

    /**
     * 获取未读消息数
     * @deprecated 使用 /total-unread 替代
     */
    @Deprecated
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<Map<String, Object>> getUnreadCount(@RequestParam Long senderId) {
        Integer unreadCount = messageService.getUnreadCount(senderId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("unreadCount", unreadCount);
        
        return Result.success(data);
    }
}
