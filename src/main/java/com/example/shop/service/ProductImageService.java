package com.example.shop.service;

import com.example.shop.entity.ProductImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 商品图片服务接口
 */
public interface ProductImageService {

    /**
     * 上传商品图片
     */
    String uploadProductImage(Long productId, MultipartFile file, Integer isMain);

    /**
     * 设置主图
     */
    void setMainImage(Long imageId, Long productId);

    /**
     * 删除商品图片
     */
    void deleteProductImage(Long imageId);

    /**
     * 根据商品ID获取图片列表
     */
    List<ProductImage> getProductImages(Long productId);

    /**
     * 根据商品ID删除所有图片
     */
    void deleteProductImages(Long productId);

    /**
     * 批量保存商品图片
     */
    void saveProductImages(Long productId, MultipartFile[] images);
}
