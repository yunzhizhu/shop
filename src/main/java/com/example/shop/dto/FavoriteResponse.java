package com.example.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏响应DTO
 */
@Data
public class FavoriteResponse {

    /**
     * 收藏ID
     */
    private Long favoriteId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品主图
     */
    private String mainImage;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

    /**
     * 商品状态(0-下架,1-上架)
     */
    private Integer status;

    /**
     * 收藏时间
     */
    private LocalDateTime createdAt;
}