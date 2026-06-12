package com.example.shop.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车商品响应DTO
 */
@Data
public class CartItemResponse {

    /**
     * 购物车ID
     */
    private Long cartId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品主图
     */
    private String mainImage;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 是否选中
     */
    private Integer isSelected;

    /**
     * 商品库存
     */
    private Integer stock;

    /**
     * 商品状态(0-下架,1-上架)
     */
    private Integer status;

    /**
     * 乐观锁版本号
     */
    private Integer version;
}
