package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("`order`")
public class Order {

    /**
     * 订单ID
     */
    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;

    /**
     * 订单编号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 订单总金额
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    @TableField("payment_amount")
    private BigDecimal paymentAmount;

    /**
     * 运费
     */
    @TableField("freight_amount")
    private BigDecimal freightAmount;

    /**
     * 使用的用户优惠券ID
     */
    @TableField("user_coupon_id")
    private Long userCouponId;

    /**
     * 优惠券抵扣金额
     */
    @TableField("coupon_amount")
    private BigDecimal couponAmount;

    /**
     * 支付方式(1-支付宝,2-微信,3-余额)
     */
    @TableField("payment_type")
    private Integer paymentType;

    /**
     * 订单状态(0-待支付,1-已支付,2-已发货,3-已完成,4-已取消)
     */
    @TableField("status")
    private Integer status;

    /**
     * 收货地址ID
     */
    @TableField("address_id")
    private Long addressId;

    /**
     * 支付时间
     */
    @TableField("payment_time")
    private LocalDateTime paymentTime;

    /**
     * 发货时间
     */
    @TableField("delivery_time")
    private LocalDateTime deliveryTime;

    /**
     * 收货时间
     */
    @TableField("receive_time")
    private LocalDateTime receiveTime;

    /**
     * 订单备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;
}
