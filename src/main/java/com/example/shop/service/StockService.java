package com.example.shop.service;

/**
 * 库存管理服务接口
 */
public interface StockService {

    /**
     * 预占库存
     */
    boolean reserveStock(Long productId, Integer quantity);

    /**
     * 释放预占库存
     */
    boolean releaseReservedStock(Long productId, Integer quantity);

    /**
     * 扣减总库存（支付时使用）
     * 同时更新MySQL和Redis
     */
    boolean deductStock(Long productId, Integer quantity);

    /**
     * 获取可用库存
     */
    Integer getAvailableStock(Long productId);

    /**
     * 获取用户购物车中某商品的数量
     */
    Integer getUserCartQuantity(Long productId, Long userId);

    /**
     * 初始化商品库存到Redis
     */
    void initStockToRedis(Long productId);

    /**
     * 清除商品库存缓存
     */
    void clearStockCache(Long productId);
}