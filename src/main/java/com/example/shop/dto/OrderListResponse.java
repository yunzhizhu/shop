package com.example.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表响应DTO
 */
@Data
public class OrderListResponse {

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    private BigDecimal paymentAmount;

    /**
     * 商品总数量
     */
    private Integer itemCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 是否全部已评价（仅已完成订单有意义）
     * 是: 所有商品都已评价，显示"已评价"
     * 否: 还有未评价商品，显示"评价"按钮
     */
    private Boolean allReviewed;

    /**
     * 已评价商品数量
     */
    private Integer reviewedCount;

    /**
     * 订单商品列表（最多显示前3个）
     */
    private List<OrderItemInfo> items;

    @Data
    public static class OrderItemInfo {
        /**
         * 商品ID
         */
        private Long productId;

        /**
         * 商品名称
         */
        private String productName;

        /**
         * 商品图片
         */
        private String productImage;

        /**
         * 购买数量
         */
        private Integer quantity;

        /**
         * 下单时价格
         */
        private BigDecimal currentPrice;
    }
}
