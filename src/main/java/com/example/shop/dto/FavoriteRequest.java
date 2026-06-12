package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 收藏请求DTO
 */
@Data
public class FavoriteRequest {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;
}