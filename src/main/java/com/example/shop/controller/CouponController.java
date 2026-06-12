package com.example.shop.controller;

import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.dto.*;
import com.example.shop.exception.BusinessException;
import com.example.shop.service.CouponService;
import com.example.shop.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 优惠券控制器
 */
@Slf4j
@RestController
@RequestMapping("/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    /**
     * 领取优惠券
     */
    @PostMapping("/receive/{couponId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "领取优惠券", module = "优惠券模块", action = "receiveCoupon")
    public Result<Map<String, Object>> receiveCoupon(@PathVariable Long couponId) {
        Long userCouponId = couponService.receiveCoupon(couponId);

        Map<String, Object> data = new HashMap<>();
        data.put("userCouponId", userCouponId);

        return Result.success("领取成功", data);
    }

    /**
     * 获取用户优惠券列表
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<UserCouponResponse>> getUserCoupons(
            @RequestParam(required = false) Integer status) {

        List<UserCouponResponse> userCoupons = couponService.getUserCoupons(status);
        return Result.success(userCoupons);
    }

    /**
     * 获取可用的优惠券(下单前)
     */
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<AvailableCouponResponse>> getAvailableCoupons(
            @RequestParam BigDecimal amount) {

        // 验证订单金额
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "订单金额必须大于0");
        }

        List<AvailableCouponResponse> availableCoupons = couponService.getAvailableCoupons(amount);
        return Result.success(availableCoupons);
    }

    /**
     * 使用优惠券(下单时)
     */
    @PostMapping("/use")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "使用优惠券", module = "优惠券模块", action = "useCoupon")
    public Result<Void> useCoupon(@RequestBody Map<String, Long> request) {
        Long userCouponId = request.get("userCouponId");
        Long orderId = request.get("orderId");

        couponService.useCoupon(userCouponId, orderId);
        return Result.success("使用成功", null);
    }

    /**
     * 退还优惠券(取消订单时)
     */
    @PostMapping("/return")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "退还优惠券", module = "优惠券模块", action = "returnCoupon")
    public Result<Void> returnCoupon(@RequestBody Map<String, Long> request) {
        Long userCouponId = request.get("userCouponId");

        couponService.returnCoupon(userCouponId);
        return Result.success("退还成功", null);
    }

    /**
     * 获取可领取的优惠券列表
     */
    @GetMapping("/list")
    public Result<List<PublicCouponResponse>> getPublicCoupons() {
        // 获取当前用户ID（如果已登录）
        Long userId = null;
        try {
            userId = SecurityUtil.getCurrentUserId();
        } catch (Exception e) {
            // 未登录用户，userId为null
        }

        List<PublicCouponResponse> coupons = couponService.getAvailableCoupons(userId);
        return Result.success(coupons);
    }
}
