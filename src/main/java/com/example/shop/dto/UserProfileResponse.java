package com.example.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 */
@Data
public class UserProfileResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 账户余额
     */
    private BigDecimal balance;

    /**
     * 积分
     */
    private Integer points;

    /**
     * 角色ID
     */
    private Integer roleId;

    /**
     * 角色名称
     */
    private String role;

    /**
     * 角色描述
     */
    private String roleDescription;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 注册时间
     */
    private LocalDateTime createdAt;
}
