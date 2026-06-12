package com.example.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 可用优惠券响应DTO
 */
@Data
public class AvailableCouponResponse {

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
     * 优惠金额/折扣率
     */
    private BigDecimal amount;

    /**
     * 使用门槛
     */
    private BigDecimal minPoint;

    /**
     * 实际优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 到期时间
     */
    private LocalDateTime endTime;
}
