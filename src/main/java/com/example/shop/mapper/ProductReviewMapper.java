package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.entity.ProductReview;
import org.apache.ibatis.annotations.Param;

/**
 * 商品评价Mapper接口
 */
public interface ProductReviewMapper extends BaseMapper<ProductReview> {

    /**
     * 分页查询商品评价列表
     */
    IPage<ProductReview> selectReviewPage(Page<ProductReview> page,
                                        @Param("productId") Long productId,
                                        @Param("rating") Integer rating);

    /**
     * 获取商品平均评分
     */
    Double selectAvgRatingByProductId(@Param("productId") Long productId);

    /**
     * 获取商品评价数量
     */
    Long selectReviewCountByProductId(@Param("productId") Long productId);

    /**
     * 检查用户是否已评价该订单商品
     */
    ProductReview selectByUserIdAndOrderIdAndProductId(@Param("userId") Long userId,
                                                      @Param("orderId") Long orderId,
                                                      @Param("productId") Long productId);

    /**
     * 根据商品ID删除所有评价
     */
    int deleteByProductId(@Param("productId") Long productId);

    /**
     * 查询订单下所有商品的评价状态（用于评价页面展示）
     */
    java.util.List<com.example.shop.dto.OrderReviewStatusResponse> selectOrderReviewStatus(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId);
}
