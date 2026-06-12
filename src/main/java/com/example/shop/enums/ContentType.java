package com.example.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 内容类型枚举
 */
@Getter
@AllArgsConstructor
public enum ContentType {
    PRIVATE_MESSAGE(1, "普通私信"),
    SYSTEM_NOTIFICATION(2, "系统通知"),
    ORDER_NOTIFICATION(3, "订单通知"),
    ACTIVITY_NOTIFICATION(4, "活动通知");

    private final Integer code;
    private final String description;

    /**
     * 根据类型码获取类型枚举
     */
    public static ContentType getByCode(Integer code) {
        for (ContentType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}