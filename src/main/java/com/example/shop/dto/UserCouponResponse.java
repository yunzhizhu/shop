package com.example.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券响应DTO
 */
@Data
public class UserCouponResponse {

    /**
     * 用户优惠券ID
     */
    private Long userCouponId;

    /**
     * 优惠券ID
     */
    private Long couponId;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 优惠券类型
     */
    private Integer type;

    /**
     * 类型描述
     */
    private String typeDesc;

    /**
     * 优惠金额/折扣率
     */
    private BigDecimal amount;

    /**
     * 使用门槛
     */
    private BigDecimal minPoint;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 获取时间
     */
    private LocalDateTime getTime;

    /**
     * 使用时间
     */
    private LocalDateTime useTime;

    /**
     * 实际优惠金额（计算后的）
     */
    private BigDecimal discountAmount;
}
