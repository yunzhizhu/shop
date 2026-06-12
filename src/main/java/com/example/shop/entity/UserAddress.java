package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户地址实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user_address")
public class UserAddress {

    /**
     * 地址ID
     */
    @TableId(value = "address_id", type = IdType.AUTO)
    private Long addressId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 收货人姓名
     */
    @TableField("receiver_name")
    private String receiverName;

    /**
     * 收货人电话
     */
    @TableField("receiver_phone")
    private String receiverPhone;

    /**
     * 省
     */
    @TableField("province")
    private String province;

    /**
     * 市
     */
    @TableField("city")
    private String city;

    /**
     * 区
     */
    @TableField("district")
    private String district;

    /**
     * 详细地址
     */
    @TableField("detail_address")
    private String detailAddress;

    /**
     * 是否默认(0-否,1-是)
     */
    @TableField("is_default")
    private Integer isDefault;
}
