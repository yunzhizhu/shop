package com.example.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 优惠券类型枚举
 */
@Getter
@AllArgsConstructor
public enum CouponType {
    FULL_REDUCTION(1, "满减券"),
    DISCOUNT(2, "折扣券"),
    NO_THRESHOLD(3, "无门槛券");

    private final Integer code;
    private final String description;

    /**
     * 根据类型码获取类型枚举
     */
    public static CouponType getByCode(Integer code) {
        for (CouponType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 计算优惠金额
     */
    public BigDecimal calculateDiscount(BigDecimal amount, BigDecimal orderAmount) {
        switch (this) {
            case FULL_REDUCTION:
            case NO_THRESHOLD:
                // 满减券和无门槛券：直接返回优惠金额
                return amount;
            case DISCOUNT:
                // 折扣券：amount 表示折扣率（如 0.98 表示 9.8折，即支付 98%）
                // 优惠金额 = 订单金额 × (1 - 折扣率)
                // 例如：订单 100 元，0.98 折扣，优惠 100 × (1 - 0.98) = 2 元
                BigDecimal discount = BigDecimal.ONE.subtract(amount);
                return orderAmount.multiply(discount).setScale(2, BigDecimal.ROUND_HALF_UP);
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * 检查是否满足使用条件
     */
    public boolean canUse(BigDecimal minPoint, BigDecimal orderAmount) {
        // 订单金额必须大于0
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (this == DISCOUNT || this == NO_THRESHOLD) {
            return true;
        }
        // 满减券需要检查门槛
        return minPoint == null || orderAmount.compareTo(minPoint) >= 0;
    }
}
