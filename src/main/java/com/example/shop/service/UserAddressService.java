package com.example.shop.service;

import com.example.shop.dto.AddAddressRequest;
import com.example.shop.entity.UserAddress;

import java.util.List;

/**
 * 用户地址服务接口
 */
public interface UserAddressService {

    /**
     * 添加地址
     */
    Long addAddress(AddAddressRequest request);

    /**
     * 获取用户地址列表
     */
    List<UserAddress> getUserAddresses();

    /**
     * 获取用户默认地址
     */
    UserAddress getDefaultAddress();

    /**
     * 更新地址
     */
    void updateAddress(Long addressId, AddAddressRequest request);

    /**
     * 删除地址
     */
    void deleteAddress(Long addressId);

    /**
     * 设置默认地址
     */
    void setDefaultAddress(Long addressId);
}
