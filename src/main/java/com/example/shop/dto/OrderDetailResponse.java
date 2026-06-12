package com.example.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情响应DTO
 */
@Data
public class OrderDetailResponse {

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户手机号
     */
    private String userPhone;

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
     * 运费
     */
    private BigDecimal freightAmount;

    /**
     * 优惠券抵扣金额
     */
    private BigDecimal couponAmount;

    /**
     * 支付方式
     */
    private Integer paymentType;

    /**
     * 支付方式描述
     */
    private String paymentTypeDesc;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;

    /**
     * 发货时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 收货时间
     */
    private LocalDateTime receiveTime;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 收货地址信息
     */
    private AddressInfo address;

    /**
     * 订单商品列表
     */
    private List<OrderItemInfo> items;

    @Data
    public static class AddressInfo {
        private String receiverName;
        private String receiverPhone;
        private String fullAddress;
    }

    @Data
    public static class OrderItemInfo {
        private Long productId;
        private String name;
        private String image;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal totalPrice;
    }
}
