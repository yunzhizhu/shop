package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("product")
public class Product {

    /**
     * 商品ID
     */
    @TableId(value = "product_id", type = IdType.AUTO)
    private Long productId;

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 分类名称（非数据库字段，用于联表查询）
     */
    @TableField(exist = false)
    private String categoryName;

    /**
     * 商品名称
     */
    @TableField("name")
    private String name;

    /**
     * 主图URL
     */
    @TableField("main_image")
    private String mainImage;

    /**
     * 商品详情
     */
    @TableField("detail")
    private String detail;

    /**
     * 商品价格
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 原价
     */
    @TableField("original_price")
    private BigDecimal originalPrice;

    /**
     * 库存数量
     */
    @TableField("stock")
    private Integer stock;

    /**
     * 预占库存
     */
    @TableField("reserved_stock")
    private Integer reservedStock;

    /**
     * 销量
     */
    @TableField("sales")
    private Integer sales;

    /**
     * 状态(0-下架,1-上架)
     */
    @TableField("status")
    private Integer status;

    /**
     * 审核状态(0-待审核PENDING, 1-审核通过APPROVED, 2-审核拒绝REJECTED)
     */
    @TableField("audit_status")
    private Integer auditStatus;

    /**
     * 审核拒绝原因
     */
    @TableField("reject_reason")
    private String rejectReason;

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
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;
}
