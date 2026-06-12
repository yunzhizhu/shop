package com.example.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息阅读状态枚举
 */
@Getter
@AllArgsConstructor
public enum MessageReadStatus {
    UNREAD(0, "未读"),
    READ(1, "已读");

    private final Integer code;
    private final String description;

    /**
     * 根据状态码获取状态枚举
     */
    public static MessageReadStatus getByCode(Integer code) {
        for (MessageReadStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
