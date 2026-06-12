package com.example.shop.controller;

import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.service.CartSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员购物车管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/cart")
public class AdminCartController {

    @Autowired
    private CartSyncService cartSyncService;

    /**
     * 同步所有购物车数据（管理员专用）
     */
    @PostMapping("/sync/all")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "同步所有购物车数据", module = "管理员模块", action = "syncAllCartData")
    public Result<Void> syncAllCartData() {
        cartSyncService.syncAllCartData();
        return Result.success("所有购物车数据同步成功", null);
    }

    /**
     * 同步指定商品的预占库存（管理员专用）
     */
    @PostMapping("/sync/product/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "同步商品预占库存", module = "管理员模块", action = "syncProductReservedStock")
    public Result<Void> syncProductReservedStock(@PathVariable Long productId) {
        cartSyncService.syncProductReservedStock(productId);
        return Result.success("商品预占库存同步成功", null);
    }

    /**
     * 同步指定用户的购物车数据（管理员专用）
     */
    @PostMapping("/sync/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "同步用户购物车数据", module = "管理员模块", action = "syncUserCartData")
    public Result<Void> syncUserCartData(@PathVariable Long userId) {
        cartSyncService.syncUserCartData(userId);
        return Result.success("用户购物车数据同步成功", null);
    }
}