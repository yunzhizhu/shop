package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 处理好友请求DTO
 */
@Data
public class HandleFriendRequestDTO {

    /**
     * 请求ID
     */
    @NotNull(message = "请求ID不能为空")
    private Long requestId;

    /**
     * 是否接受(true-接受,false-拒绝)
     */
    @NotNull(message = "处理结果不能为空")
    private Boolean accept;
}
