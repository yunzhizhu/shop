package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user")
public class User {

    /**
     * 用户ID
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码哈希值
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 密码盐值
     */
    @TableField("salt")
    private String salt;

    /**
     * 头像URL
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 手机号码
     */
    @TableField("phone")
    private String phone;

    /**
     * 角色ID(1:管理员,2:普通用户)
     */
    @TableField("role_id")
    private Integer roleId;

    /**
     * 账户余额
     */
    @TableField("balance")
    private BigDecimal balance;

    /**
     * 积分
     */
    @TableField("points")
    private Integer points;

    /**
     * 状态(0:禁用,1:正常)
     */
    @TableField("status")
    private Integer status;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
