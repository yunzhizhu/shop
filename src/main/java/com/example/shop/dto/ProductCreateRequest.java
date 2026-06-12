package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建商品请求DTO
 */
@Data
public class ProductCreateRequest {

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    private String name;

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /**
     * 商品价格
     */
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格最低为0.01元")
    @DecimalMax(value = "999999.99", message = "商品价格不能超过999999.99元")
    @Digits(integer = 6, fraction = 2, message = "价格格式不正确，最多6位整数和2位小数")
    private BigDecimal price;

    /**
     * 原价（可选）
     */
    @DecimalMin(value = "0.01", message = "原价最低为0.01元")
    @DecimalMax(value = "999999.99", message = "原价不能超过999999.99元")
    @Digits(integer = 6, fraction = 2, message = "原价格式不正确，最多6位整数和2位小数")
    private BigDecimal originalPrice;

    /**
     * 库存数量
     */
    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能小于0")
    private Integer stock;

    /**
     * 商品详情
     */
    private String detail;
}
