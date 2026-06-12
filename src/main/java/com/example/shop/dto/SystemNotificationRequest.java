package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.*;

/**
 * 系统通知请求DTO
 */
@Data
public class SystemNotificationRequest {

    /**
     * 接收者ID
     */
    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;

    /**
     * 通知内容
     */
    @NotBlank(message = "通知内容不能为空")
    @Size(max = 1000, message = "通知内容不能超过1000个字符")
    private String content;

    /**
     * 内容类型(2-系统通知,3-订单通知,4-活动通知)
     */
    @NotNull(message = "内容类型不能为空")
    @Min(value = 2, message = "内容类型必须为2-4之间")
    @Max(value = 4, message = "内容类型必须为2-4之间")
    private Integer contentType;

    /**
     * 消息类型(1-文本,2-图片)
     */
    @NotNull(message = "消息类型不能为空")
    @Min(value = 1, message = "消息类型必须为1或2")
    @Max(value = 2, message = "消息类型必须为1或2")
    private Integer messageType;

    /**
     * 图片URL(仅图片消息需要)
     */
    private String imageUrl;
}