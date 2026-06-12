package com.example.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品状态枚举
 */
@Getter
@AllArgsConstructor
public enum ProductStatus {
    OFF_SHELF(0, "下架"),
    ON_SHELF(1, "上架");

    private final Integer code;
    private final String description;

    /**
     * 根据状态码获取状态枚举
     */
    public static ProductStatus getByCode(Integer code) {
        for (ProductStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
