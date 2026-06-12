package com.example.shop.service.impl;

import com.example.shop.constants.RedisConstants;
import com.example.shop.entity.ProductImage;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.ProductImageMapper;
import com.example.shop.service.ProductImageService;
import com.example.shop.service.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 商品图片服务实现类
 */
@Slf4j
@Service
public class ProductImageServiceImpl implements ProductImageService {

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private com.example.shop.mapper.ProductMapper productMapper;

    @Autowired
    private com.example.shop.helper.FileUploadHelper fileUploadHelper;

    @Autowired
    private RedisCacheService redisCacheService;

    /**
     * 清除商品相关缓存
     */
    private void clearProductCache(Long productId) {
        redisCacheService.delete(RedisConstants.getProductInfoKey(productId));
        redisCacheService.delete(RedisConstants.getProductImagesKey(productId));
        log.debug("已清除商品缓存: productId={}", productId);
    }

    @Override
    @Transactional
    public String uploadProductImage(Long productId, MultipartFile file, Integer isMain) {
        // 使用FileUploadHelper验证文件
        fileUploadHelper.validateFile(file);

        try {
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String fileName = UUID.randomUUID().toString() + "." + fileExtension;

            // 创建上传目录
            fileUploadHelper.createDirectoryIfNotExists(fileUploadHelper.getProductPath());

            // 保存文件
            Path filePath = fileUploadHelper.buildFilePath(fileUploadHelper.getProductPath(), fileName);
            Files.copy(file.getInputStream(), filePath);

            // 构建访问URL
            String imageUrl = fileUploadHelper.getProductAccessUrl(fileName);

            // 如果是主图，先清除其他主图标记
            if (isMain != null && isMain == 1) {
                productImageMapper.clearMainImageByProductId(productId);
            }

            // 保存图片信息到数据库
            ProductImage productImage = new ProductImage();
            productImage.setProductId(productId);
            productImage.setImageUrl(imageUrl);
            productImage.setIsMain(isMain != null ? isMain : 0);
            productImage.setSortOrder(0);

            int result = productImageMapper.insert(productImage);
            if (result <= 0) {
                throw new BusinessException("图片信息保存失败");
            }

            log.info("商品图片上传成功: productId={}, imageUrl={}", productId, imageUrl);

            // 清除商品缓存
            clearProductCache(productId);

            return imageUrl;

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    @Transactional
    public void setMainImage(Long imageId, Long productId) {
        // 检查图片是否存在
        ProductImage image = productImageMapper.selectById(imageId);
        if (image == null) {
            throw new BusinessException(404, "图片不存在");
        }

        // 检查图片是否属于该商品
        if (!image.getProductId().equals(productId)) {
            throw new BusinessException(400, "图片不属于该商品");
        }

        // 先清除该商品的所有主图标记
        productImageMapper.clearMainImageByProductId(productId);

        // 设置新的主图
        int result = productImageMapper.setMainImage(imageId, productId);
        if (result <= 0) {
            throw new BusinessException("设置主图失败");
        }

        // 同步更新商品表的主图字段
        productMapper.updateMainImage(productId, image.getImageUrl());

        log.info("设置主图成功: imageId={}, productId={}, imageUrl={}", imageId, productId, image.getImageUrl());

        // 清除商品缓存
        clearProductCache(productId);
    }

    @Override
    @Transactional
    public void deleteProductImage(Long imageId) {
        // 检查图片是否存在
        ProductImage image = productImageMapper.selectById(imageId);
        if (image == null) {
            throw new BusinessException(404, "图片不存在");
        }

        // 删除数据库记录
        int result = productImageMapper.deleteById(imageId);
        if (result <= 0) {
            throw new BusinessException("图片删除失败");
        }

        // 删除物理文件
        try {
            String imageUrl = image.getImageUrl();
            String urlPrefix = fileUploadHelper.getProductUrlPrefix();
            String fileName = fileUploadHelper.extractFileName(imageUrl, urlPrefix);
            if (fileName != null) {
                Path filePath = fileUploadHelper.buildFilePath(fileUploadHelper.getProductPath(), fileName);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.warn("删除物理文件失败: {}", e.getMessage());
        }

        log.info("商品图片删除成功: imageId={}", imageId);

        // 清除商品缓存
        clearProductCache(image.getProductId());
    }

    @Override
    public List<ProductImage> getProductImages(Long productId) {
        return productImageMapper.selectByProductId(productId);
    }

    @Override
    @Transactional
    public void deleteProductImages(Long productId) {
        // 获取所有图片
        List<ProductImage> images = productImageMapper.selectByProductId(productId);

        // 删除物理文件
        for (ProductImage image : images) {
            try {
                String imageUrl = image.getImageUrl();
                String urlPrefix = fileUploadHelper.getProductUrlPrefix();
                String fileName = fileUploadHelper.extractFileName(imageUrl, urlPrefix);
                if (fileName != null) {
                    Path filePath = fileUploadHelper.buildFilePath(fileUploadHelper.getProductPath(), fileName);
                    Files.deleteIfExists(filePath);
                }
            } catch (IOException e) {
                log.warn("删除物理文件失败: {}", e.getMessage());
            }
        }

        // 删除数据库记录
        productImageMapper.deleteByProductId(productId);

        log.info("商品所有图片删除成功: productId={}", productId);
    }

    @Override
    @Transactional
    public void saveProductImages(Long productId, MultipartFile[] images) {
        if (images == null || images.length == 0) {
            return;
        }

        for (MultipartFile image : images) {
            if (!image.isEmpty()) {
                uploadProductImage(productId, image, 0);
            }
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
}
