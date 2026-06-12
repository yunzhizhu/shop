package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("coupon")
public class Coupon {

    /**
     * 优惠券ID
     */
    @TableId(value = "coupon_id", type = IdType.AUTO)
    private Long couponId;

    /**
     * 优惠券名称
     */
    @TableField("name")
    private String name;

    /**
     * 优惠券类型(1-满减,2-折扣,3-无门槛)
     */
    @TableField("type")
    private Integer type;

    /**
     * 优惠金额/折扣率
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 使用门槛
     */
    @TableField("min_point")
    private BigDecimal minPoint;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 发行数量
     */
    @TableField("total_count")
    private Integer totalCount;

    /**
     * 已领取数量
     */
    @TableField("received_count")
    private Integer receivedCount;

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
}
