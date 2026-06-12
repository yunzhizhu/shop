package com.example.shop.dto;

import lombok.Data;

/**
 * 用户搜索响应DTO
 */
@Data
public class UserSearchResponse {

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
     * 手机号（部分显示）
     */
    private String phone;

    /**
     * 是否已是好友
     */
    private Boolean isFriend;

    /**
     * 是否已发送请求
     */
    private Boolean hasRequestPending;
}
