package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shop.dto.FavoriteResponse;
import com.example.shop.entity.UserFavorite;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户收藏Mapper接口
 */
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /**
     * 根据用户ID查询收藏列表
     */
    List<UserFavorite> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和商品ID查询收藏记录
     */
    UserFavorite selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 删除用户收藏
     */
    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 查询用户收藏列表（包含商品信息）
     */
    List<FavoriteResponse> selectFavoriteListWithProduct(@Param("userId") Long userId);

    /**
     * 批量删除用户收藏
     */
    int batchDeleteByUserIdAndProductIds(@Param("userId") Long userId, @Param("productIds") List<Long> productIds);
}
