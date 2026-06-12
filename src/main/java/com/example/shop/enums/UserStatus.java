package com.example.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 */
@Getter
@AllArgsConstructor
public enum UserStatus {
    DISABLED(0, "禁用"),
    NORMAL(1, "正常");

    private final Integer code;
    private final String description;

    /**
     * 根据状态码获取状态枚举
     */
    public static UserStatus getByCode(Integer code) {
        for (UserStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
