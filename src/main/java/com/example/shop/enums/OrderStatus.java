package com.example.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {
    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String description;

    /**
     * 根据状态码获取状态枚举
     */
    public static OrderStatus getByCode(Integer code) {
        for (OrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 检查是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING_PAYMENT;
    }

    /**
     * 检查是否可以支付
     */
    public boolean canPay() {
        return this == PENDING_PAYMENT;
    }

    /**
     * 检查是否可以发货
     */
    public boolean canDeliver() {
        return this == PAID;
    }

    /**
     * 检查是否可以确认收货
     */
    public boolean canReceive() {
        return this == SHIPPED;
    }
}
