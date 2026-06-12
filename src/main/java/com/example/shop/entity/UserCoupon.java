package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户优惠券实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user_coupon")
public class UserCoupon {

    /**
     * 主键ID
     */
    @TableId(value = "user_coupon_id", type = IdType.AUTO)
    private Long userCouponId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 优惠券ID
     */
    @TableField("coupon_id")
    private Long couponId;



    /**
     * 状态(0-未使用,1-已使用,2-已过期)
     */
    @TableField("status")
    private Integer status;

    /**
     * 获取时间
     */
    @TableField("get_time")
    private LocalDateTime getTime;

    /**
     * 使用时间
     */
    @TableField("use_time")
    private LocalDateTime useTime;

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
