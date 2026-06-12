package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.entity.Coupon;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券Mapper接口
 */
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 分页查询优惠券列表
     */
    IPage<Coupon> selectCouponPage(Page<Coupon> page,
                                 @Param("name") String name,
                                 @Param("type") Integer type,
                                 @Param("status") Integer status);

    /**
     * 获取可领取的优惠券列表
     */
    List<Coupon> selectAvailableCoupons(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 增加已领取数量
     */
    int increaseReceivedCount(@Param("couponId") Long couponId);

    /**
     * 减少已领取数量
     */
    int decreaseReceivedCount(@Param("couponId") Long couponId);

    /**
     * 检查优惠券是否可以领取
     */
    boolean canReceive(@Param("couponId") Long couponId, @Param("currentTime") LocalDateTime currentTime);

    /**
     * 获取优惠券剩余数量
     */
    Integer getRemainingCount(@Param("couponId") Long couponId);
}
