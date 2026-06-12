package com.example.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付方式枚举
 */
@Getter
@AllArgsConstructor
public enum PaymentType {
    ALIPAY(1, "支付宝"),
    WECHAT(2, "微信支付"),
    BALANCE(3, "余额支付");

    private final Integer code;
    private final String description;

    /**
     * 根据支付方式码获取支付方式枚举
     */
    public static PaymentType getByCode(Integer code) {
        for (PaymentType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
