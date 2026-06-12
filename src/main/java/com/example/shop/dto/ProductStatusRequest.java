package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 商品状态更新请求DTO
 */
@Data
public class ProductStatusRequest {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 状态(0-下架,1-上架)
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 乐观锁版本号
     */
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
