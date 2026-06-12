package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.*;

/**
 * 发送消息请求DTO
 */
@Data
public class SendMessageRequest {

    /**
     * 接收者ID
     */
    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1000, message = "消息内容不能超过1000个字符")
    private String content;

    /**
     * 消息类型(1-文本,2-图片)
     */
    @NotNull(message = "消息类型不能为空")
    @Min(value = 1, message = "消息类型必须为1或2")
    @Max(value = 2, message = "消息类型必须为1或2")
    private Integer messageType;

    /**
     * 内容类型(1-普通私信,2-系统通知,3-订单通知,4-活动通知)
     * 默认为1(普通私信)
     */
    private Integer contentType = 1;

    /**
     * 图片URL(仅图片消息需要)
     */
    private String imageUrl;
}
