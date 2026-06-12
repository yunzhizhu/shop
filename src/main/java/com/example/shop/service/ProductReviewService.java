package com.example.shop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.dto.ReviewCreateRequest;
import com.example.shop.entity.ProductReview;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品评价服务接口
 */
public interface ProductReviewService {

    /**
     * 创建评价
     */
    Long createReview(ReviewCreateRequest request, MultipartFile[] images);

    /**
     * 分页查询商品评价列表
     */
    IPage<ProductReview> getReviewPage(int page, int size, Long productId, Integer rating);

    /**
     * 删除评价（管理员）
     */
    void deleteReview(Long reviewId);

    /**
     * 获取商品平均评分
     */
    Double getAvgRating(Long productId);

    /**
     * 获取商品评价数量
     */
    Long getReviewCount(Long productId);

    /**
     * 检查用户是否可以评价
     */
    boolean canUserReview(Long userId, Long orderId, Long productId);

    /**
     * 获取订单下所有商品的评价状态（用于评价页面）
     */
    java.util.List<com.example.shop.dto.OrderReviewStatusResponse> getOrderReviewStatus(Long orderId);
}
