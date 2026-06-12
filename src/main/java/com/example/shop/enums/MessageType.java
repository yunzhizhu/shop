package com.example.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型枚举
 */
@Getter
@AllArgsConstructor
public enum MessageType {
    TEXT(1, "文本消息"),
    IMAGE(2, "图片消息");

    private final Integer code;
    private final String description;

    /**
     * 根据类型码获取类型枚举
     */
    public static MessageType getByCode(Integer code) {
        for (MessageType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
