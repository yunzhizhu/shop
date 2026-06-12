package com.example.shop.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.constants.RedisConstants;
import com.example.shop.dto.ReviewCreateRequest;
import com.example.shop.entity.Order;
import com.example.shop.entity.ProductReview;
import com.example.shop.enums.OrderStatus;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.OrderMapper;
import com.example.shop.mapper.ProductReviewMapper;
import com.example.shop.service.ProductReviewService;
import com.example.shop.service.RedisCacheService;
import com.example.shop.utils.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 商品评价服务实现类
 * v2.0 添加Redis缓存，提升查询性能
 */
@Slf4j
@Service
public class ProductReviewServiceImpl implements ProductReviewService {

    @Autowired
    private ProductReviewMapper productReviewMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.example.shop.helper.FileUploadHelper fileUploadHelper;
    
    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    @Transactional
    public Long createReview(ReviewCreateRequest request, MultipartFile[] images) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 检查用户是否可以评价
        if (!canUserReview(userId, request.getOrderId(), request.getProductId())) {
            throw new BusinessException(400, "您无法评价此商品或已经评价过了");
        }

        // 处理评价图片
        List<String> imageUrls = new ArrayList<>();
        if (images != null && images.length > 0) {
            imageUrls = uploadReviewImages(images);
        }

        // 创建评价
        ProductReview review = new ProductReview();
        review.setProductId(request.getProductId());
        review.setUserId(userId);
        review.setOrderId(request.getOrderId());
        review.setRating(request.getRating());
        review.setContent(request.getContent());

        // 将图片URL列表转换为JSON字符串
        if (!imageUrls.isEmpty()) {
            try {
                review.setImages(objectMapper.writeValueAsString(imageUrls));
            } catch (Exception e) {
                log.error("图片URL序列化失败: {}", e.getMessage());
                throw new BusinessException("评价创建失败");
            }
        }

        int result = productReviewMapper.insert(review);
        if (result <= 0) {
            throw new BusinessException("评价创建失败");
        }

        // 清除评论相关缓存
        clearReviewCache(request.getProductId());

        log.info("评价创建成功: reviewId={}, productId={}, userId={}", 
                review.getReviewId(), request.getProductId(), userId);
        return review.getReviewId();
    }

    @Override
    public IPage<ProductReview> getReviewPage(int page, int size, Long productId, Integer rating) {
        Page<ProductReview> pageParam = new Page<>(page, size);
        IPage<ProductReview> result = productReviewMapper.selectReviewPage(pageParam, productId, rating);
        result.getRecords().forEach(r -> {
            // 补全头像完整URL
            r.setAvatar(fileUploadHelper.toFullUrl(r.getAvatar()));
            // 替换评价图片JSON中的旧域名
            if (r.getImages() != null && !r.getImages().isEmpty()) {
                try {
                    List<String> urls = objectMapper.readValue(r.getImages(), List.class);
                    List<String> fixedUrls = urls.stream()
                            .map(fileUploadHelper::toFullUrl)
                            .collect(java.util.stream.Collectors.toList());
                    r.setImages(objectMapper.writeValueAsString(fixedUrls));
                } catch (Exception e) {
                    log.warn("评价图片URL转换失败: reviewId={}", r.getReviewId());
                }
            }
        });
        return result;
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        // 检查评价是否存在
        ProductReview review = productReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(404, "评价不存在");
        }

        // 删除评价图片文件
        if (review.getImages() != null) {
            try {
                List<String> imageUrls = objectMapper.readValue(review.getImages(), List.class);
                for (String imageUrl : imageUrls) {
                    deleteReviewImage(imageUrl);
                }
            } catch (Exception e) {
                log.warn("删除评价图片失败: {}", e.getMessage());
            }
        }

        // 删除评价记录
        int result = productReviewMapper.deleteById(reviewId);
        if (result <= 0) {
            throw new BusinessException("评价删除失败");
        }

        // 清除评论相关缓存
        clearReviewCache(review.getProductId());

        log.info("评价删除成功: reviewId={}", reviewId);
    }

    @Override
    public Double getAvgRating(Long productId) {
        // 尝试从缓存获取评论统计
        String cacheKey = RedisConstants.getReviewStatsKey(productId);
        ReviewStats stats = redisCacheService.get(cacheKey, ReviewStats.class);
        
        if (stats != null) {
            log.debug("评论统计缓存命中: productId={}", productId);
            return stats.getAvgRating();
        }
        
        // 缓存未命中，从数据库查询
        Double avgRating = productReviewMapper.selectAvgRatingByProductId(productId);
        Long reviewCount = productReviewMapper.selectReviewCountByProductId(productId);
        
        // 缓存到Redis
        stats = new ReviewStats(avgRating, reviewCount);
        redisCacheService.set(cacheKey, stats, RedisConstants.REVIEW_STATS_TTL, TimeUnit.SECONDS);
        log.debug("评论统计已缓存: productId={}, avgRating={}, count={}", productId, avgRating, reviewCount);
        
        return avgRating;
    }

    @Override
    public Long getReviewCount(Long productId) {
        // 尝试从缓存获取评论统计
        String cacheKey = RedisConstants.getReviewStatsKey(productId);
        ReviewStats stats = redisCacheService.get(cacheKey, ReviewStats.class);
        
        if (stats != null) {
            log.debug("评论统计缓存命中: productId={}", productId);
            return stats.getReviewCount();
        }
        
        // 缓存未命中，从数据库查询
        Double avgRating = productReviewMapper.selectAvgRatingByProductId(productId);
        Long reviewCount = productReviewMapper.selectReviewCountByProductId(productId);
        
        // 缓存到Redis
        stats = new ReviewStats(avgRating, reviewCount);
        redisCacheService.set(cacheKey, stats, RedisConstants.REVIEW_STATS_TTL, TimeUnit.SECONDS);
        log.debug("评论统计已缓存: productId={}, avgRating={}, count={}", productId, avgRating, reviewCount);
        
        return reviewCount;
    }

    @Override
    public boolean canUserReview(Long userId, Long orderId, Long productId) {
        // 检查用户是否已经评价过该订单的该商品
        ProductReview existingReview = productReviewMapper.selectByUserIdAndOrderIdAndProductId(
                userId, orderId, productId);
        if (existingReview != null) {
            return false;
        }

        // 验证订单是否存在、属于当前用户、且状态为已完成
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        if (!order.getUserId().equals(userId)) {
            return false;
        }
        // 只有已完成的订单才能评价
        if (!OrderStatus.COMPLETED.getCode().equals(order.getStatus())) {
            return false;
        }

        return true;
    }

    @Override
    public java.util.List<com.example.shop.dto.OrderReviewStatusResponse> getOrderReviewStatus(Long orderId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return productReviewMapper.selectOrderReviewStatus(orderId, userId);
    }

    /**
     * 上传评价图片
     */
    private List<String> uploadReviewImages(MultipartFile[] images) {
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile image : images) {
            if (!image.isEmpty()) {
                String imageUrl = uploadReviewImage(image);
                imageUrls.add(imageUrl);
            }
        }

        return imageUrls;
    }

    /**
     * 上传单个评价图片
     */
    private String uploadReviewImage(MultipartFile file) {
        // 使用FileUploadHelper验证文件
        fileUploadHelper.validateFile(file);

        try {
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String fileName = UUID.randomUUID().toString() + "." + fileExtension;

            // 创建上传目录
            fileUploadHelper.createDirectoryIfNotExists(fileUploadHelper.getReviewPath());

            // 保存文件
            Path filePath = fileUploadHelper.buildFilePath(fileUploadHelper.getReviewPath(), fileName);
            Files.copy(file.getInputStream(), filePath);

            // 存储相对路径，查询时动态拼接域名
            return fileUploadHelper.getReviewAccessUrl(fileName);

        } catch (IOException e) {
            log.error("评价图片上传失败: {}", e.getMessage(), e);
            throw new BusinessException("图片上传失败");
        }
    }

    /**
     * 删除评价图片
     */
    private void deleteReviewImage(String imageUrl) {
        try {
            String urlPrefix = fileUploadHelper.getReviewUrlPrefix();
            String fileName = fileUploadHelper.extractFileName(imageUrl, urlPrefix);
            if (fileName != null) {
                Path filePath = fileUploadHelper.buildFilePath(fileUploadHelper.getReviewPath(), fileName);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.warn("删除评价图片文件失败: {}", e.getMessage());
        }
    }



    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
    
    /**
     * 清除评论缓存
     */
    private void clearReviewCache(Long productId) {
        // 清除评论统计缓存
        String statsKey = RedisConstants.getReviewStatsKey(productId);
        redisCacheService.delete(statsKey);
        
        // 清除评论列表缓存（所有分页）
        // 注意：这里简化处理，实际可以使用Redis的scan命令批量删除
        // 或者使用更精细的缓存键管理策略
        String listKeyPattern = RedisConstants.REVIEW_LIST_KEY + productId + ":*";
        // 由于RedisCacheService没有批量删除方法，这里只清除统计缓存
        // 评论列表缓存会在10分钟后自动过期
        
        log.debug("评论缓存已清除: productId={}", productId);
    }
    
    /**
     * 评论统计内部类
     */
    private static class ReviewStats {
        private Double avgRating;
        private Long reviewCount;
        
        public ReviewStats() {}
        
        public ReviewStats(Double avgRating, Long reviewCount) {
            this.avgRating = avgRating;
            this.reviewCount = reviewCount;
        }
        
        public Double getAvgRating() {
            return avgRating;
        }
        
        public void setAvgRating(Double avgRating) {
            this.avgRating = avgRating;
        }
        
        public Long getReviewCount() {
            return reviewCount;
        }
        
        public void setReviewCount(Long reviewCount) {
            this.reviewCount = reviewCount;
        }
    }
}
