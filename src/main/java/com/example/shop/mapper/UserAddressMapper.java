package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shop.entity.UserAddress;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户地址Mapper接口
 */
public interface UserAddressMapper extends BaseMapper<UserAddress> {

    /**
     * 根据用户ID查询地址列表
     */
    List<UserAddress> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询默认地址
     */
    UserAddress selectDefaultByUserId(@Param("userId") Long userId);

    /**
     * 清除用户的所有默认地址
     */
    int clearDefaultByUserId(@Param("userId") Long userId);

    /**
     * 设置默认地址
     */
    int setDefaultAddress(@Param("addressId") Long addressId, @Param("userId") Long userId);
}
