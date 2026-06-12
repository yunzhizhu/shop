package com.example.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品列表项响应DTO
 */
@Data
public class ProductListItemResponse {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 主图URL
     */
    private String mainImage;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 审核状态(0-待审核, 1-审核通过, 2-审核拒绝)
     */
    private Integer auditStatus;

    /**
     * 审核拒绝原因
     */
    private String rejectReason;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 商品图片列表
     */
    private List<ProductImageInfo> images;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 平均评分
     */
    private Double avgRating;

    /**
     * 评价数量
     */
    private Long reviewCount;

    @Data
    public static class ProductImageInfo {
        /**
         * 图片ID
         */
        private Long imageId;
        
        /**
         * 图片URL
         */
        private String url;
        
        /**
         * 是否为主图
         */
        private Boolean main;
        
        /**
         * 排序顺序
         */
        private Integer sortOrder;
    }
}
