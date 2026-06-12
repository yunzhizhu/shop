package com.example.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员订单列表响应DTO
 */
@Data
public class AdminOrderListResponse {

    /**
     * 订单ID
     */
    private Long orderId;

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
    private String phone;

    /**
     * 订单状态(0-待支付,1-已支付,2-已发货,3-已完成,4-已取消)
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
     * 支付方式(1-支付宝,2-微信,3-余额)
     */
    private Integer paymentType;

    /**
     * 支付方式描述
     */
    private String paymentTypeDesc;

    /**
     * 收货人姓名
     */
    private String receiverName;

    /**
     * 收货人手机号
     */
    private String receiverPhone;

    /**
     * 商品数量
     */
    private Integer itemCount;

    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;

    /**
     * 发货时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
