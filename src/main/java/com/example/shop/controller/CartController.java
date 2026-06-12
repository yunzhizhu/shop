package com.example.shop.controller;

import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.dto.*;
import com.example.shop.service.CartService;
import com.example.shop.service.CartSyncService;
import com.example.shop.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 购物车控制器
 */
@Slf4j
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartSyncService cartSyncService;

    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "添加购物车", module = "购物车模块", action = "addToCart")
    public Result<Map<String, Object>> addToCart(@Valid @RequestBody CartAddRequest request) {
        Long cartId = cartService.addToCart(request);
        Integer totalItems = cartService.getCartItemCount();
        
        Map<String, Object> data = new HashMap<>();
        data.put("cartId", cartId);
        data.put("totalItems", totalItems);
        
        return Result.success("添加成功", data);
    }

    /**
     * 修改购物车商品数量
     */
    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "更新购物车", module = "购物车模块", action = "updateCartItem")
    public Result<Void> updateCartItem(@Valid @RequestBody CartUpdateRequest request) {
        cartService.updateCartItem(request);
        return Result.success("更新成功", null);
    }

    /**
     * 设置选中状态
     */
    @PutMapping("/select")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "设置购物车选中状态", module = "购物车模块", action = "updateSelectedStatus")
    public Result<Void> updateSelectedStatus(@Valid @RequestBody CartSelectRequest request) {
        cartService.updateSelectedStatus(request);
        return Result.success("操作成功", null);
    }

    /**
     * 获取购物车列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<CartItemResponse>> getCartList() {
        List<CartItemResponse> cartItems = cartService.getCartItems();
        return Result.success(cartItems);
    }

    /**
     * 删除购物车商品
     */
    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "删除购物车商品", module = "购物车模块", action = "deleteCartItems")
    public Result<Void> deleteCartItems(@RequestBody Map<String, List<Long>> request) {
        List<Long> cartIds = request.get("cartIds");
        cartService.deleteCartItems(cartIds);
        return Result.success("删除成功", null);
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("/clear")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "清空购物车", module = "购物车模块", action = "clearCart")
    public Result<Void> clearCart() {
        cartService.clearCart();
        return Result.success("清空成功", null);
    }

    /**
     * 获取购物车商品总数
     * 未登录用户返回0，已登录用户返回实际数量
     */
    @GetMapping("/count")
    public Result<Integer> getCartItemCount() {
        try {
            Integer count = cartService.getCartItemCount();
            return Result.success(count);
        } catch (Exception e) {
            // 未登录或其他异常情况，返回0
            log.debug("获取购物车数量失败，返回0: {}", e.getMessage());
            return Result.success(0);
        }
    }

    /**
     * 校验购物车商品库存
     */
    @GetMapping("/validate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<CartValidationResult>> validateCart() {
        List<CartValidationResult> results = cartService.validateCartItems();
        return Result.success(results);
    }

    /**
     * 获取选中的购物车商品（用于订单确认页）
     */
    @GetMapping("/selected")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<CartItemResponse>> getSelectedCartItems() {
        List<CartItemResponse> items = cartService.getSelectedCartItems();
        return Result.success(items);
    }

    /**
     * 校验选中的购物车商品库存（结算前校验）
     */
    @GetMapping("/validate/selected")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<List<CartValidationResult>> validateSelectedCart() {
        List<CartValidationResult> results = cartService.validateSelectedCartItems();
        return Result.success(results);
    }

    /**
     * 同步购物车数据（修复数据不一致问题）
     */
    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "同步购物车数据", module = "购物车模块", action = "syncCartData")
    public Result<Void> syncCartData() {
        Long userId = SecurityUtil.getCurrentUserId();
        cartSyncService.syncUserCartData(userId);
        return Result.success("购物车数据同步成功", null);
    }

    /**
     * 强制修复购物车数据不一致问题
     */
    @PostMapping("/force-fix")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "强制修复购物车数据", module = "购物车模块", action = "forceFixCartData")
    public Result<Void> forceFixCartData() {
        Long userId = SecurityUtil.getCurrentUserId();
        cartSyncService.forceFixCartDataInconsistency(userId);
        return Result.success("购物车数据修复成功", null);
    }
}
