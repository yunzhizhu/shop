package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 创建订单请求DTO
 */
@Data
public class CreateOrderRequest {

    /**
     * 收货地址ID
     */
    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;

    /**
     * 使用的用户优惠券ID
     */
    private Long userCouponId;

    /**
     * 订单备注
     */
    private String remark;
}
