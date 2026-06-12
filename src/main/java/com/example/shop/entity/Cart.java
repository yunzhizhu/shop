package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 购物车实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cart")
public class Cart {

    /**
     * 购物车ID
     */
    @TableId(value = "cart_id", type = IdType.AUTO)
    private Long cartId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 商品ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 商品数量
     */
    @TableField("quantity")
    private Integer quantity;

    /**
     * 是否选中(0-未选中,1-已选中)
     */
    @TableField("is_selected")
    private Integer isSelected;

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
