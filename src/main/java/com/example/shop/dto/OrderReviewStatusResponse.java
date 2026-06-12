package com.example.shop.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单评价状态响应DTO
 * 用于评价页面展示每个商品的评价状态
 */
@Data
public class OrderReviewStatusResponse {

    /** 订单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品图片 */
    private String productImage;

    /** 购买数量 */
    private Integer quantity;

    /** 下单时价格 */
    private BigDecimal currentPrice;

    /** 是否已评价 */
    private Boolean hasReviewed;

    /** 已有评价ID（已评价时有值） */
    private Long reviewId;

    /** 已有评分（已评价时有值） */
    private Integer rating;

    /** 已有评价内容（已评价时有值） */
    private String content;
}
