package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建分类请求DTO
 */
@Data
public class CategoryCreateRequest {

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /**
     * 父分类ID
     */
    @NotNull(message = "父分类ID不能为空")
    private Long parentId;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 状态（0-禁用，1-启用）
     */
    private Integer status;
}
