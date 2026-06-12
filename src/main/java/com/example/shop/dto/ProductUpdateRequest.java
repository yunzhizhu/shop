package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 更新商品请求DTO
 */
@Data
public class ProductUpdateRequest {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 商品价格
     */
    @DecimalMin(value = "0.01", message = "商品价格最低为0.01元")
    @DecimalMax(value = "999999.99", message = "商品价格不能超过999999.99元")
    @Digits(integer = 6, fraction = 2, message = "价格格式不正确，最多6位整数和2位小数")
    private BigDecimal price;

    /**
     * 原价
     */
    @DecimalMax(value = "999999.99", message = "原价不能超过999999.99元")
    @Digits(integer = 6, fraction = 2, message = "原价格式不正确，最多6位整数和2位小数")
    private BigDecimal originalPrice;

    /**
     * 是否删除原价（true-删除原价，false或null-不删除）
     */
    private Boolean clearOriginalPrice;

    /**
     * 库存数量
     */
    @Min(value = 0, message = "库存数量不能小于0")
    private Integer stock;

    /**
     * 商品详情
     */
    private String detail;

    /**
     * 商品状态(0-下架,1-上架)
     */
    private Integer status;

    /**
     * 乐观锁版本号
     */
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
