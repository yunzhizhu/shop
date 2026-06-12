package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 更新购物车请求DTO
 */
@Data
public class CartUpdateRequest {

    /**
     * 购物车ID
     */
    @NotNull(message = "购物车ID不能为空")
    private Long cartId;

    /**
     * 商品数量
     */
    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量必须大于0")
    private Integer quantity;

    /**
     * 乐观锁版本号
     */
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
