package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品图片实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("product_image")
public class ProductImage {

    /**
     * 图片ID
     */
    @TableId(value = "image_id", type = IdType.AUTO)
    private Long imageId;

    /**
     * 商品ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 图片URL
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 是否主图(0-否,1-是)
     */
    @TableField("is_main")
    private Integer isMain;

    /**
     * 排序权重(越大越靠前)
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
