package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 更新优惠券请求DTO
 */
@Data
public class CouponUpdateRequest {

    /**
     * 优惠券ID
     */
    @NotNull(message = "优惠券ID不能为空")
    private Long couponId;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 优惠券类型(1-满减,2-折扣,3-无门槛)
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
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 发行数量
     */
    private Integer totalCount;

    /**
     * 状态(0-禁用,1-启用)
     */
    private Integer status;
}
