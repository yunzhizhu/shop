package com.example.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户优惠券状态枚举
 */
@Getter
@AllArgsConstructor
public enum UserCouponStatus {
    UNUSED(0, "未使用"),
    USED(1, "已使用"),
    EXPIRED(2, "已过期");

    private final Integer code;
    private final String description;

    /**
     * 根据状态码获取状态枚举
     */
    public static UserCouponStatus getByCode(Integer code) {
        for (UserCouponStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 检查是否可以使用
     */
    public boolean canUse() {
        return this == UNUSED;
    }
}
