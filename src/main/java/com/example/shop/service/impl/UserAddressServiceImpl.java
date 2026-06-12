package com.example.shop.service.impl;

import com.example.shop.dto.AddAddressRequest;
import com.example.shop.entity.UserAddress;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.UserAddressMapper;
import com.example.shop.service.UserAddressService;
import com.example.shop.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户地址服务实现类
 */
@Slf4j
@Service
public class UserAddressServiceImpl implements UserAddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Override
    @Transactional
    public Long addAddress(AddAddressRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 如果设置为默认地址，先检查是否已有默认地址
        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            UserAddress defaultAddress = userAddressMapper.selectDefaultByUserId(userId);
            if (defaultAddress != null) {
                throw new BusinessException(1004, "已存在默认地址，请先取消当前默认地址");
            }
        }

        UserAddress address = new UserAddress();
        BeanUtils.copyProperties(request, address);
        address.setUserId(userId);
        
        // 如果没有指定是否默认，且用户没有其他地址，则设为默认
        if (request.getIsDefault() == null) {
            List<UserAddress> existingAddresses = userAddressMapper.selectByUserId(userId);
            address.setIsDefault(existingAddresses.isEmpty() ? 1 : 0);
        }

        int result = userAddressMapper.insert(address);
        if (result <= 0) {
            throw new BusinessException("地址添加失败");
        }

        log.info("用户地址添加成功: userId={}, addressId={}", userId, address.getAddressId());
        return address.getAddressId();
    }

    @Override
    public List<UserAddress> getUserAddresses() {
        Long userId = SecurityUtil.getCurrentUserId();
        return userAddressMapper.selectByUserId(userId);
    }

    @Override
    public UserAddress getDefaultAddress() {
        Long userId = SecurityUtil.getCurrentUserId();
        return userAddressMapper.selectDefaultByUserId(userId);
    }

    @Override
    @Transactional
    public void updateAddress(Long addressId, AddAddressRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException(404, "地址不存在");
        }

        // 检查地址是否属于当前用户
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作此地址");
        }

        // 如果要设置为默认地址，先清除其他默认地址
        if (request.getIsDefault() != null && request.getIsDefault() == 1 && address.getIsDefault() != 1) {
            userAddressMapper.clearDefaultByUserId(userId);
        }

        BeanUtils.copyProperties(request, address);
        address.setAddressId(addressId);
        address.setUserId(userId);

        int result = userAddressMapper.updateById(address);
        if (result <= 0) {
            throw new BusinessException("地址更新失败");
        }

        log.info("用户地址更新成功: userId={}, addressId={}", userId, addressId);
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException(404, "地址不存在");
        }

        // 检查地址是否属于当前用户
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作此地址");
        }

        int result = userAddressMapper.deleteById(addressId);
        if (result <= 0) {
            throw new BusinessException("地址删除失败");
        }

        log.info("用户地址删除成功: userId={}, addressId={}", userId, addressId);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long addressId) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException(404, "地址不存在");
        }

        // 检查地址是否属于当前用户
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作此地址");
        }

        // 先清除用户的所有默认地址
        userAddressMapper.clearDefaultByUserId(userId);

        // 设置新的默认地址
        int result = userAddressMapper.setDefaultAddress(addressId, userId);
        if (result <= 0) {
            throw new BusinessException("设置默认地址失败");
        }

        log.info("设置默认地址成功: userId={}, addressId={}", userId, addressId);
    }
}
