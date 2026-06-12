package com.example.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.shop.dto.FavoriteResponse;
import com.example.shop.entity.Product;
import com.example.shop.entity.UserFavorite;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.mapper.UserFavoriteMapper;
import com.example.shop.service.FavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 收藏服务实现类
 */
@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private com.example.shop.helper.FileUploadHelper fileUploadHelper;

    @Override
    @Transactional
    public boolean addFavorite(Long userId, Long productId) {
        try {
            // 检查商品是否存在
            Product product = productMapper.selectById(productId);
            if (product == null) {
                log.warn("商品不存在，productId: {}", productId);
                return false;
            }

            // 检查是否已收藏
            UserFavorite existingFavorite = userFavoriteMapper.selectByUserIdAndProductId(userId, productId);
            if (existingFavorite != null) {
                log.warn("商品已收藏，userId: {}, productId: {}", userId, productId);
                return false;
            }

            // 添加收藏
            UserFavorite favorite = new UserFavorite();
            favorite.setUserId(userId);
            favorite.setProductId(productId);
            
            int result = userFavoriteMapper.insert(favorite);
            log.info("添加收藏成功，userId: {}, productId: {}", userId, productId);
            return result > 0;
        } catch (Exception e) {
            log.error("添加收藏失败，userId: {}, productId: {}", userId, productId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean removeFavorite(Long userId, Long productId) {
        try {
            int result = userFavoriteMapper.deleteByUserIdAndProductId(userId, productId);
            log.info("取消收藏成功，userId: {}, productId: {}", userId, productId);
            return result > 0;
        } catch (Exception e) {
            log.error("取消收藏失败，userId: {}, productId: {}", userId, productId, e);
            return false;
        }
    }

    @Override
    public boolean isFavorite(Long userId, Long productId) {
        UserFavorite favorite = userFavoriteMapper.selectByUserIdAndProductId(userId, productId);
        return favorite != null;
    }

    @Override
    public List<FavoriteResponse> getFavoriteList(Long userId) {
        try {
            List<FavoriteResponse> favoriteList = userFavoriteMapper.selectFavoriteListWithProduct(userId);
            
            // 转换商品主图为完整URL
            favoriteList.forEach(item -> {
                if (item.getMainImage() != null && !item.getMainImage().isEmpty()) {
                    item.setMainImage(fileUploadHelper.toFullUrl(item.getMainImage()));
                }
            });
            
            log.info("获取收藏列表成功，userId: {}, count: {}", userId, favoriteList.size());
            return favoriteList;
        } catch (Exception e) {
            log.error("获取收藏列表失败，userId: {}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public long getFavoriteCount(Long userId) {
        try {
            QueryWrapper<UserFavorite> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId);
            long count = userFavoriteMapper.selectCount(queryWrapper);
            log.info("获取收藏数量成功，userId: {}, count: {}", userId, count);
            return count;
        } catch (Exception e) {
            log.error("获取收藏数量失败，userId: {}", userId, e);
            return 0;
        }
    }

    @Override
    @Transactional
    public int batchRemoveFavorites(Long userId, List<Long> productIds) {
        try {
            if (productIds == null || productIds.isEmpty()) {
                log.warn("商品ID列表为空，userId: {}", userId);
                return 0;
            }

            int result = userFavoriteMapper.batchDeleteByUserIdAndProductIds(userId, productIds);
            log.info("批量删除收藏成功，userId: {}, productIds: {}, deletedCount: {}", userId, productIds, result);
            return result;
        } catch (Exception e) {
            log.error("批量删除收藏失败，userId: {}, productIds: {}", userId, productIds, e);
            return 0;
        }
    }
}