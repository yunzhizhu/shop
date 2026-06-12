package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品分类实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("category")
public class Category {

    /**
     * 分类ID
     */
    @TableId(value = "category_id", type = IdType.AUTO)
    private Long categoryId;

    /**
     * 分类名称
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * 父分类ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 排序权重
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态(0-禁用,1-启用)
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 子分类列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Category> children;
}
