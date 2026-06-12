package com.example.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会话类型枚举
 */
@Getter
@AllArgsConstructor
public enum ConversationType {
    PRIVATE_CHAT(1, "私信会话"),
    SYSTEM_NOTIFICATION(2, "系统通知会话");

    private final Integer code;
    private final String description;

    /**
     * 根据类型码获取类型枚举
     */
    public static ConversationType getByCode(Integer code) {
        for (ConversationType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}