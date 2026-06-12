package com.example.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 公共优惠券响应DTO
 */
@Data
public class PublicCouponResponse {

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
     * 剩余数量
     */
    private Integer remaining;

    /**
     * 是否已领取
     */
    private Boolean received;
}
