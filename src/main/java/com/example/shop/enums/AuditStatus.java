package com.example.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品审核状态枚举
 */
@Getter
@AllArgsConstructor
public enum AuditStatus {
    PENDING(0, "待审核"),
    APPROVED(1, "审核通过"),
    REJECTED(2, "审核拒绝");

    private final Integer code;
    private final String description;

    public static AuditStatus getByCode(Integer code) {
        for (AuditStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
