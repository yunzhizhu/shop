package com.example.shop.service;

import com.example.shop.entity.Cart;

import java.util.List;

/**
 * 购物车Redis操作服务接口
 * 负责购物车数据的Redis缓存操作
 * 
 * @author Kiro
 * @version 1.0
 * @since 2026-03-07
 */
public interface CartRedisService {

    /**
     * 获取用户购物车（所有商品）
     * 
     * @param userId 用户ID
     * @return 购物车列表，如果不存在返回null
     */
    List<Cart> getUserCart(Long userId);

    /**
     * 获取单个购物车项
     * 
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 购物车项，如果不存在返回null
     */
    Cart getCartItem(Long userId, Long productId);

    /**
     * 保存/更新购物车项
     * 
     * @param userId 用户ID
     * @param cart 购物车项
     */
    void saveCartItem(Long userId, Cart cart);

    /**
     * 批量保存购物车项
     * 
     * @param userId 用户ID
     * @param carts 购物车列表
     */
    void saveCartItems(Long userId, List<Cart> carts);

    /**
     * 删除购物车项
     * 
     * @param userId 用户ID
     * @param productId 商品ID
     */
    void deleteCartItem(Long userId, Long productId);

    /**
     * 批量删除购物车项
     * 
     * @param userId 用户ID
     * @param productIds 商品ID列表
     */
    void deleteCartItems(Long userId, List<Long> productIds);

    /**
     * 清空用户购物车
     * 
     * @param userId 用户ID
     */
    void clearCart(Long userId);

    /**
     * 检查用户购物车是否存在于Redis
     * 
     * @param userId 用户ID
     * @return true-存在，false-不存在
     */
    boolean existsCart(Long userId);

    /**
     * 刷新购物车缓存过期时间
     * 
     * @param userId 用户ID
     */
    void refreshExpire(Long userId);

    /**
     * 从MySQL加载购物车数据到Redis
     * 
     * @param userId 用户ID
     */
    void loadFromDatabase(Long userId);

    /**
     * 原子性增加购物车商品数量
     * 使用Lua脚本保证原子性
     * 
     * @param userId 用户ID
     * @param productId 商品ID
     * @param quantity 增加的数量
     * @param cart 购物车项（如果不存在则新增）
     * @return 更新后的数量
     */
    Integer incrementQuantity(Long userId, Long productId, Integer quantity, Cart cart);
}
