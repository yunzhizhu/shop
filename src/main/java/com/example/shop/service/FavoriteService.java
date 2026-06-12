package com.example.shop.service;

import com.example.shop.dto.FavoriteResponse;

import java.util.List;

/**
 * 收藏服务接口
 */
public interface FavoriteService {

    /**
     * 添加收藏
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 是否成功
     */
    boolean addFavorite(Long userId, Long productId);

    /**
     * 取消收藏
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 是否成功
     */
    boolean removeFavorite(Long userId, Long productId);

    /**
     * 检查是否已收藏
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 是否已收藏
     */
    boolean isFavorite(Long userId, Long productId);

    /**
     * 获取用户收藏列表
     * @param userId 用户ID
     * @return 收藏列表
     */
    List<FavoriteResponse> getFavoriteList(Long userId);

    /**
     * 获取用户收藏数量
     * @param userId 用户ID
     * @return 收藏数量
     */
    long getFavoriteCount(Long userId);

    /**
     * 批量删除收藏
     * @param userId 用户ID
     * @param productIds 商品ID列表
     * @return 删除成功的数量
     */
    int batchRemoveFavorites(Long userId, List<Long> productIds);
}