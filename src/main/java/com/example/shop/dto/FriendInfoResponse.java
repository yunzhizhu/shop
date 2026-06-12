package com.example.shop.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友信息响应DTO
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class FriendInfoResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否拉黑
     */
    private Integer isBlocked;

    /**
     * 成为好友时间
     */
    private LocalDateTime createdAt;
}
