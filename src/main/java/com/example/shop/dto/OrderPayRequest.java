package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 订单支付请求DTO
 */
@Data
public class OrderPayRequest {

    /**
     * 订单编号
     */
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    /**
     * 支付方式(1-支付宝,2-微信,3-余额)
     */
    @NotNull(message = "支付方式不能为空")
    private Integer paymentType;
}
