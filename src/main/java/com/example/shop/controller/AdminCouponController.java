package com.example.shop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.dto.CouponCreateRequest;
import com.example.shop.dto.CouponUpdateRequest;
import com.example.shop.entity.Coupon;
import com.example.shop.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员优惠券控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/coupon")
public class AdminCouponController {

    @Autowired
    private CouponService couponService;

    /**
     * 创建优惠券
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "创建优惠券", module = "优惠券模块", action = "createCoupon")
    public Result<Map<String, Object>> createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        Long couponId = couponService.createCoupon(request);
        
        Map<String, Object> data = new HashMap<>();
        data.put("couponId", couponId);
        
        return Result.success("创建成功", data);
    }

    /**
     * 修改优惠券
     */
    @PutMapping("/update")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "修改优惠券", module = "优惠券模块", action = "updateCoupon")
    public Result<Void> updateCoupon(@RequestBody CouponUpdateRequest request) {
        couponService.updateCoupon(request);
        return Result.success("修改成功", null);
    }

    /**
     * 获取优惠券列表(分页)
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<Coupon>> getCouponList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {
        
        IPage<Coupon> couponPage = couponService.getCouponPage(page, size, name, type, status);
        return Result.success(couponPage);
    }

    /**
     * 获取优惠券详情
     */
    @GetMapping("/detail/{couponId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Coupon> getCouponDetail(@PathVariable Long couponId) {
        Coupon coupon = couponService.getCouponById(couponId);
        return Result.success(coupon);
    }

    /**
     * 禁用优惠券
     */
    @PutMapping("/disable/{couponId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "禁用优惠券", module = "优惠券模块", action = "disableCoupon")
    public Result<Void> disableCoupon(@PathVariable Long couponId) {
        couponService.disableCoupon(couponId);
        return Result.success("禁用成功", null);
    }

    /**
     * 启用优惠券
     */
    @PutMapping("/enable/{couponId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "启用优惠券", module = "优惠券模块", action = "enableCoupon")
    public Result<Void> enableCoupon(@PathVariable Long couponId) {
        couponService.enableCoupon(couponId);
        return Result.success("启用成功", null);
    }

    /**
     * 切换优惠券状态
     */
    @PutMapping("/toggle-status/{couponId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "切换优惠券状态", module = "优惠券模块", action = "toggleCouponStatus")
    public Result<Map<String, Object>> toggleCouponStatus(@PathVariable Long couponId) {
        Integer newStatus = couponService.toggleCouponStatus(couponId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("couponId", couponId);
        data.put("status", newStatus);
        data.put("statusDesc", newStatus == 1 ? "启用" : "禁用");
        
        String message = newStatus == 1 ? "启用成功" : "禁用成功";
        return Result.success(message, data);
    }
}
