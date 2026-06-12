package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建优惠券请求DTO
 */
@Data
public class CouponCreateRequest {

    /**
     * 优惠券名称
     */
    @NotBlank(message = "优惠券名称不能为空")
    @Size(max = 100, message = "优惠券名称不能超过100个字符")
    private String name;

    /**
     * 优惠券类型(1-满减,2-折扣,3-无门槛)
     */
    @NotNull(message = "优惠券类型不能为空")
    @Min(value = 1, message = "优惠券类型必须为1、2或3")
    @Max(value = 3, message = "优惠券类型必须为1、2或3")
    private Integer type;

    /**
     * 优惠金额/折扣率
     */
    @NotNull(message = "优惠金额不能为空")
    @DecimalMin(value = "0.01", message = "优惠金额必须大于0")
    private BigDecimal amount;

    /**
     * 使用门槛
     */
    @DecimalMin(value = "0.01", message = "使用门槛必须大于0")
    private BigDecimal minPoint;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间必须是未来时间")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    @Future(message = "结束时间必须是未来时间")
    private LocalDateTime endTime;

    /**
     * 发行数量
     */
    @NotNull(message = "发行数量不能为空")
    @Min(value = 1, message = "发行数量必须大于0")
    private Integer totalCount;
}
