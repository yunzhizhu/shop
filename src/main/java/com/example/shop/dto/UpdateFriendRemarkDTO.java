package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 更新好友备注DTO
 */
@Data
public class UpdateFriendRemarkDTO {

    /**
     * 好友ID
     */
    @NotNull(message = "好友ID不能为空")
    private Long friendId;

    /**
     * 备注名称
     */
    private String remark;
}
