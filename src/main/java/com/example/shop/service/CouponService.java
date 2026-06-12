package com.example.shop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.dto.*;
import com.example.shop.entity.Coupon;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券服务接口
 */
public interface CouponService {

    /**
     * 创建优惠券
     */
    Long createCoupon(CouponCreateRequest request);

    /**
     * 更新优惠券
     */
    void updateCoupon(CouponUpdateRequest request);

    /**
     * 分页查询优惠券列表
     */
    IPage<Coupon> getCouponPage(int page, int size, String name, Integer type, Integer status);

    /**
     * 根据ID获取优惠券详情
     */
    Coupon getCouponById(Long couponId);

    /**
     * 禁用优惠券
     */
    void disableCoupon(Long couponId);

    /**
     * 启用优惠券
     */
    void enableCoupon(Long couponId);

    /**
     * 切换优惠券状态
     */
    Integer toggleCouponStatus(Long couponId);

    /**
     * 获取可领取的优惠券列表
     */
    List<PublicCouponResponse> getAvailableCoupons(Long userId);

    /**
     * 领取优惠券
     */
    Long receiveCoupon(Long couponId);

    /**
     * 获取用户优惠券列表
     */
    List<UserCouponResponse> getUserCoupons(Integer status);

    /**
     * 获取可用的优惠券（下单前）
     */
    List<AvailableCouponResponse> getAvailableCoupons(BigDecimal orderAmount);

    /**
     * 使用优惠券
     */
    void useCoupon(Long userCouponId, Long orderId);

    /**
     * 退还优惠券
     */
    void returnCoupon(Long userCouponId);

    /**
     * 计算优惠金额
     */
    BigDecimal calculateDiscount(Long userCouponId, BigDecimal orderAmount);

    /**
     * 检查优惠券是否可用
     */
    boolean canUseCoupon(Long userCouponId, BigDecimal orderAmount);

    /**
     * 更新过期优惠券状态
     */
    void updateExpiredCoupons();
}
