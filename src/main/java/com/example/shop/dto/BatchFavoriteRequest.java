package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量收藏操作请求DTO
 */
@Data
public class BatchFavoriteRequest {

    /**
     * 商品ID列表
     */
    @NotEmpty(message = "商品ID列表不能为空")
    private List<Long> productIds;
}