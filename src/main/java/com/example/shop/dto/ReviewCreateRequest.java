package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 创建评价请求DTO
 */
@Data
public class ReviewCreateRequest {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 评分(1-5星)
     */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1星")
    @Max(value = 5, message = "评分最高为5星")
    private Integer rating;

    /**
     * 评价内容
     */
    private String content;
}
