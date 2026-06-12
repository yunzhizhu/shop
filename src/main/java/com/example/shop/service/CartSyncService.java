package com.example.shop.service;

/**
 * 购物车数据同步服务接口
 */
public interface CartSyncService {

    /**
     * 同步用户购物车数据
     * 确保购物车数量与预占库存一致
     */
    void syncUserCartData(Long userId);

    /**
     * 修复所有用户的购物车数据
     * 用于系统维护
     */
    void syncAllCartData();

    /**
     * 检查并修复单个商品的预占库存
     */
    void syncProductReservedStock(Long productId);

    /**
     * 强制修复购物车数据不一致问题
     */
    void forceFixCartDataInconsistency(Long userId);
}