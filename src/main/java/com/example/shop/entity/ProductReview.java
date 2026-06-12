package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商品评价实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("product_review")
public class ProductReview {

    /**
     * 评价ID
     */
    @TableId(value = "review_id", type = IdType.AUTO)
    private Long reviewId;

    /**
     * 商品ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 订单ID
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 评分(1-5星)
     */
    @TableField("rating")
    private Integer rating;

    /**
     * 评价内容
     */
    @TableField("content")
    private String content;

    /**
     * 评价图片(JSON数组存储URL)
     */
    @TableField("images")
    private String images;

    /**
     * 用户名（关联查询，非数据库字段）
     */
    @TableField(exist = false)
    private String username;

    /**
     * 用户头像（关联查询，非数据库字段）
     */
    @TableField(exist = false)
    private String avatar;

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
}
