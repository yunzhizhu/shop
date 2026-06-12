package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shop.dto.CartItemResponse;
import com.example.shop.entity.Cart;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物车Mapper接口
 */
public interface CartMapper extends BaseMapper<Cart> {

    /**
     * 根据用户ID和商品ID查询购物车项
     */
    Cart selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 获取用户购物车列表（包含商品信息）
     */
    List<CartItemResponse> selectCartItemsByUserId(@Param("userId") Long userId);

    /**
     * 获取用户选中的购物车项
     */
    List<Cart> selectSelectedCartItems(@Param("userId") Long userId);

    /**
     * 批量更新选中状态
     */
    int updateSelectedStatus(@Param("cartIds") List<Long> cartIds, 
                           @Param("userId") Long userId, 
                           @Param("selected") Integer selected);

    /**
     * 根据用户ID删除购物车项
     */
    int deleteByUserIdAndCartIds(@Param("userId") Long userId, @Param("cartIds") List<Long> cartIds);

    /**
     * 删除用户选中的购物车项
     */
    int deleteSelectedItems(@Param("userId") Long userId);

    /**
     * 获取用户购物车商品总数
     */
    Integer countCartItems(@Param("userId") Long userId);

    /**
     * 获取用户某商品在购物车中的数量
     */
    Integer getUserCartQuantity(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 获取某商品在所有购物车中的总数量
     */
    Integer getTotalCartQuantityByProduct(@Param("productId") Long productId);
}
