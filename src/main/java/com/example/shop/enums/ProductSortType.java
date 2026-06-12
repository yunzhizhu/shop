package com.example.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品排序类型枚举
 */
@Getter
@AllArgsConstructor
public enum ProductSortType {
    PRICE_ASC("price_asc", "价格升序"),
    PRICE_DESC("price_desc", "价格降序"),
    SALES_DESC("sales_desc", "销量降序"),
    RATING_DESC("rating_desc", "评分降序"),
    CREATE_TIME_DESC("create_time_desc", "创建时间降序");

    private final String code;
    private final String description;

    /**
     * 根据排序码获取排序枚举
     */
    public static ProductSortType getByCode(String code) {
        for (ProductSortType sortType : values()) {
            if (sortType.getCode().equals(code)) {
                return sortType;
            }
        }
        return null;
    }
}
