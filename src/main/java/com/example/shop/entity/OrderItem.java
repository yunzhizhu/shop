package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单商品实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("order_item")
public class OrderItem {

    /**
     * 订单商品ID
     */
    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;

    /**
     * 订单ID
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 订单编号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 商品ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 商品名称
     */
    @TableField("product_name")
    private String productName;

    /**
     * 商品图片
     */
    @TableField("product_image")
    private String productImage;

    /**
     * 下单时价格
     */
    @TableField("current_price")
    private BigDecimal currentPrice;

    /**
     * 购买数量
     */
    @TableField("quantity")
    private Integer quantity;

    /**
     * 商品总价
     */
    @TableField("total_price")
    private BigDecimal totalPrice;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;
}
