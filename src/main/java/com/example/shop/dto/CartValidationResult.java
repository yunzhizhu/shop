package com.example.shop.dto;

import lombok.Data;

/**
 * 购物车校验结果DTO
 */
@Data
public class CartValidationResult {

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
    private String productName;

    /**
     * 请求数量
     */
    private Integer requestQuantity;

    /**
     * 可用库存
     */
    private Integer availableStock;

    /**
     * 是否有效
     */
    private Boolean isValid;

    /**
     * 建议信息
     */
    private String suggestion;

    /**
     * 错误类型
     */
    private String errorType;

    /**
     * 建议数量
     */
    private Integer suggestedQuantity;
}