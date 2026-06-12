package com.example.shop.service;

import com.example.shop.dto.*;

import java.util.List;

/**
 * 购物车服务接口
 */
public interface CartService {

    /**
     * 添加商品到购物车
     */
    Long addToCart(CartAddRequest request);

    /**
     * 更新购物车商品数量
     */
    void updateCartItem(CartUpdateRequest request);

    /**
     * 设置购物车商品选中状态
     */
    void updateSelectedStatus(CartSelectRequest request);

    /**
     * 获取用户购物车列表
     */
    List<CartItemResponse> getCartItems();

    /**
     * 删除购物车商品
     */
    void deleteCartItems(List<Long> cartIds);

    /**
     * 清空购物车
     */
    void clearCart();

    /**
     * 获取购物车商品总数
     */
    Integer getCartItemCount();

    /**
     * 获取选中的购物车商品
     */
    List<CartItemResponse> getSelectedCartItems();

    /**
     * 校验购物车商品库存
     */
    List<CartValidationResult> validateCartItems();

    /**
     * 校验选中的购物车商品库存
     */
    List<CartValidationResult> validateSelectedCartItems();
}
