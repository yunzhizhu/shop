package com.example.shop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.dto.ReviewCreateRequest;
import com.example.shop.entity.ProductReview;
import com.example.shop.service.ProductReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品评价控制器 */
@Slf4j
@RestController
@RequestMapping("/review")
public class ReviewController {

    @Autowired
    private ProductReviewService productReviewService;

    /**
     * 获取订单评价状态（用于评价页面，展示所有商品及其评价状态）
     */
    @GetMapping("/order-status/{orderId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<java.util.List<com.example.shop.dto.OrderReviewStatusResponse>> getOrderReviewStatus(
            @PathVariable Long orderId) {
        return Result.success(productReviewService.getOrderReviewStatus(orderId));
    }

    /**
     * 提交评价
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "提交商品评价", module = "评价模块", action = "createReview")
    public Result<Map<String, Object>> createReview(
            @Valid @ModelAttribute ReviewCreateRequest request,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {

        Long reviewId = productReviewService.createReview(request, images);

        Map<String, Object> data = new HashMap<>();
        data.put("reviewId", reviewId);

        return Result.success("评价成功", data);
    }

    /**
     * 获取商品评价列表
     */
    @GetMapping("/list")
    public Result<IPage<ProductReview>> getReviewList(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer rating) {

        IPage<ProductReview> reviewPage = productReviewService.getReviewPage(page, size, productId, rating);
        return Result.success(reviewPage);
    }

    /**
     * 删除评价(管理�?
     */
    @DeleteMapping("/admin/delete/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "删除商品评价", module = "评价模块", action = "deleteReview")
    public Result<Void> deleteReview(@PathVariable Long reviewId) {
        productReviewService.deleteReview(reviewId);
        return Result.success("删除成功", null);
    }
}
