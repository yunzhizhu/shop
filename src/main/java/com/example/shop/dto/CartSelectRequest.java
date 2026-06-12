package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 购物车选中状态请求DTO
 */
@Data
public class CartSelectRequest {

    /**
     * 购物车ID列表
     */
    @NotEmpty(message = "购物车ID列表不能为空")
    private List<Long> cartIds;

    /**
     * 是否选中(0-未选中,1-已选中)
     */
    @NotNull(message = "选中状态不能为空")
    private Integer selected;
}
