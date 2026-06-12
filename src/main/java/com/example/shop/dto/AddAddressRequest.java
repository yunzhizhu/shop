package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 添加地址请求DTO
 */
@Data
public class AddAddressRequest {

    /**
     * 收货人姓名
     */
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 50, message = "收货人姓名长度不能超过50个字符")
    private String receiverName;

    /**
     * 收货人电话
     */
    @NotBlank(message = "收货人电话不能为空")
    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    /**
     * 省
     */
    @NotBlank(message = "省份不能为空")
    @Size(max = 50, message = "省份长度不能超过50个字符")
    private String province;

    /**
     * 市
     */
    @NotBlank(message = "城市不能为空")
    @Size(max = 50, message = "城市长度不能超过50个字符")
    private String city;

    /**
     * 区
     */
    @NotBlank(message = "区县不能为空")
    @Size(max = 50, message = "区县长度不能超过50个字符")
    private String district;

    /**
     * 详细地址
     */
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址长度不能超过255个字符")
    private String detailAddress;

    /**
     * 是否默认(0-否,1-是)
     */
    private Integer isDefault;
}
