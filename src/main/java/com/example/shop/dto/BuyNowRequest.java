package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 立即购买请求DTO
 */
@Data
public class BuyNowRequest {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 购买数量
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

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