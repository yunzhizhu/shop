package com.example.shop.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.constants.RedisConstants;
import com.example.shop.dto.*;
import com.example.shop.entity.Product;
import com.example.shop.entity.ProductImage;
import com.example.shop.enums.AuditStatus;
import com.example.shop.enums.ProductStatus;
import com.example.shop.enums.UserRole;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.ProductImageMapper;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.mapper.ProductReviewMapper;
import com.example.shop.service.ProductImageService;
import com.example.shop.service.ProductService;
import com.example.shop.service.RedisCacheService;
import com.example.shop.service.StockService;
import com.example.shop.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现类
 * v2.0 添加Redis缓存支持
 */
@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private ProductReviewMapper productReviewMapper;

    @Autowired
    private ProductImageService productImageService;

    @Autowired
    private com.example.shop.helper.FileUploadHelper fileUploadHelper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private StockService stockService;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    @Transactional
    public Long createProduct(ProductCreateRequest request, MultipartFile mainImage, MultipartFile[] images) {
        // 获取当前用户信息
        String currentRole = getCurrentUserRole();

        // 验证只有管理员可以创建商品
        if (!UserRole.ADMIN.getRoleName().equals(currentRole)) {
            throw new BusinessException(403, "只有管理员可以创建商品");
        }

        // 验证原价和商品价格的关系
        if (request.getOriginalPrice() != null && request.getPrice() != null) {
            if (request.getOriginalPrice().compareTo(request.getPrice()) < 0) {
                throw new BusinessException(400, "原价不能低于商品价格");
            }
        }

        // 创建商品对象
        Product product = new Product();
        BeanUtils.copyProperties(request, product);

        // 设置默认值
        product.setSales(0);
        // 新商品进入待审核状态，保持下架
        product.setAuditStatus(AuditStatus.PENDING.getCode());
        product.setStatus(ProductStatus.OFF_SHELF.getCode());

        // 保存商品
        int result = productMapper.insert(product);
        if (result <= 0) {
            throw new BusinessException("商品创建失败");
        }

        // 处理主图
        String mainImageUrl = null;
        if (mainImage != null && !mainImage.isEmpty()) {
            mainImageUrl = productImageService.uploadProductImage(product.getProductId(), mainImage, 1);
            product.setMainImage(mainImageUrl);
            productMapper.updateById(product);
        }

        // 处理其他图片
        if (images != null && images.length > 0) {
            productImageService.saveProductImages(product.getProductId(), images);

            // 如果没有上传主图，将第一张图片设为主图
            if (mainImageUrl == null) {
                List<ProductImage> productImages = productImageMapper.selectByProductId(product.getProductId());
                if (!productImages.isEmpty()) {
                    ProductImage firstImage = productImages.get(0);
                    // 设置为主图
                    productImageMapper.clearMainImageByProductId(product.getProductId());
                    productImageMapper.setMainImage(firstImage.getImageId(), product.getProductId());
                    // 更新商品表的主图字段
                    product.setMainImage(firstImage.getImageUrl());
                    productMapper.updateById(product);
                    log.info("自动设置第一张图片为主图: productId={}, imageUrl={}",
                            product.getProductId(), firstImage.getImageUrl());
                }
            }
        }

        log.info("商品创建成功: productId={}", product.getProductId());

        // 初始化库存到Redis
        stockService.initStockToRedis(product.getProductId());
        log.info("库存已初始化到Redis: productId={}, stock={}",
                product.getProductId(), product.getStock());

        // 清除相关缓存
        clearProductCache(product.getProductId(), product.getCategoryId());

        return product.getProductId();
    }

    /**
     * 清除商品相关缓存
     */
    private void clearProductCache(Long productId, Long categoryId) {
        // 清除商品详情缓存
        redisCacheService.delete(RedisConstants.getProductInfoKey(productId));
        // 清除商品图片缓存
        redisCacheService.delete(RedisConstants.getProductImagesKey(productId));
        // 清除分类商品列表缓存（简单粗暴，清除所有分页）
        // 实际生产环境可以使用模糊匹配删除
        log.debug("已清除商品缓存: productId={}, categoryId={}", productId, categoryId);
    }

    @Override
    @Transactional
    public Integer updateProduct(ProductUpdateRequest request) {
        // 检查商品是否存在
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }

        // 检查权限
        if (!checkProductPermission(request.getProductId(), SecurityUtil.getCurrentUserId(), getCurrentUserRole())) {
            throw new BusinessException(403, "无权限操作此商品");
        }

        // 处理原价删除逻辑：使用clearOriginalPrice标记
        boolean clearOriginalPrice = Boolean.TRUE.equals(request.getClearOriginalPrice());

        // 验证原价的有效性（如果不是删除操作）
        if (request.getOriginalPrice() != null && !clearOriginalPrice) {
            if (request.getOriginalPrice().compareTo(new BigDecimal("0.01")) < 0) {
                throw new BusinessException(400, "原价最低为0.01元");
            }
        }

        // 验证原价和商品价格的关系
        BigDecimal finalPrice = request.getPrice() != null ? request.getPrice() : product.getPrice();
        BigDecimal finalOriginalPrice;

        if (clearOriginalPrice) {
            finalOriginalPrice = null;  // 删除原价
        } else {
            finalOriginalPrice = request.getOriginalPrice() != null ? request.getOriginalPrice() : product.getOriginalPrice();
        }

        if (finalOriginalPrice != null && finalPrice != null) {
            if (finalOriginalPrice.compareTo(finalPrice) < 0) {
                throw new BusinessException(400, "原价不能低于商品价格");
            }
        }

        // 使用UpdateWrapper来支持更新null值
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Product> updateWrapper =
            new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        updateWrapper.eq("product_id", request.getProductId())
                    .eq("version", request.getVersion());  // 乐观锁

        // 更新商品信息
        if (request.getName() != null) {
            updateWrapper.set("name", request.getName());
        }
        if (request.getCategoryId() != null) {
            updateWrapper.set("category_id", request.getCategoryId());
        }
        if (request.getPrice() != null) {
            updateWrapper.set("price", request.getPrice());
        }
        // 处理原价更新或删除
        if (clearOriginalPrice) {
            updateWrapper.set("original_price", null);  // 明确设置为null
        } else if (request.getOriginalPrice() != null) {
            updateWrapper.set("original_price", request.getOriginalPrice());
        }

        // 记录是否更新了库存
        boolean stockUpdated = false;
        Integer oldStock = product.getStock();

        if (request.getStock() != null) {
            updateWrapper.set("stock", request.getStock());
            stockUpdated = true;
        }
        if (request.getDetail() != null) {
            updateWrapper.set("detail", request.getDetail());
        }

        // 更新商品时重置审核状态为 PENDING，商品下架
        updateWrapper.set("audit_status", AuditStatus.PENDING.getCode());
        updateWrapper.set("status", ProductStatus.OFF_SHELF.getCode());

        int result = productMapper.update(null, updateWrapper);
        if (result <= 0) {
            throw new BusinessException("商品更新失败，可能是版本冲突");
        }

        // 如果更新了库存，同步到Redis
        if (stockUpdated) {
            String stockKey = RedisConstants.getStockKey(request.getProductId());
            redisCacheService.set(stockKey, request.getStock(), 7 * 24 * 60 * 60, TimeUnit.SECONDS);
            log.info("库存已同步到Redis: productId={}, oldStock={}, newStock={}",
                    request.getProductId(), oldStock, request.getStock());
        }

        // 重新查询获取更新后的版本号
        Product updatedProduct = productMapper.selectById(request.getProductId());
        Integer newVersion = updatedProduct.getVersion();

        log.info("商品更新成功: productId={}, newVersion={}", request.getProductId(), newVersion);

        // 清除商品缓存
        clearProductCache(request.getProductId(), updatedProduct.getCategoryId());

        return newVersion;
    }

    @Override
    @Transactional
    public void updateProductStatus(ProductStatusRequest request) {
        // 检查商品是否存在
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }

        // 检查权限
        if (!checkProductPermission(request.getProductId(), SecurityUtil.getCurrentUserId(), getCurrentUserRole())) {
            throw new BusinessException(403, "无权限操作此商品");
        }

        // 验证状态值
        ProductStatus status = ProductStatus.getByCode(request.getStatus());
        if (status == null) {
            throw new BusinessException(400, "无效的商品状态");
        }

        // 上架前校验审核状态必须为 APPROVED
        if (ProductStatus.ON_SHELF.getCode().equals(request.getStatus())) {
            if (!AuditStatus.APPROVED.getCode().equals(product.getAuditStatus())) {
                throw new BusinessException(400, "商品未通过审核，无法上架");
            }
        }

        // 更新状态
        product.setStatus(request.getStatus());
        product.setVersion(request.getVersion());

        int result = productMapper.updateById(product);
        if (result <= 0) {
            throw new BusinessException("状态更新失败，可能是版本冲突");
        }

        log.info("商品状态更新成功: productId={}, status={}", request.getProductId(), request.getStatus());

        // 清除商品缓存
        clearProductCache(request.getProductId(), product.getCategoryId());
    }

    @Override
    @Transactional
    public void deleteProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new BusinessException(400, "商品ID列表不能为空");
        }

        String currentRole = getCurrentUserRole();
        Long currentUserId = SecurityUtil.getCurrentUserId();

        for (Long productId : productIds) {
            // 检查权限
            if (!checkProductPermission(productId, currentUserId, currentRole)) {
                throw new BusinessException(403, "无权限删除商品: " + productId);
            }

            // 获取商品信息（用于清除分类缓存）
            Product product = productMapper.selectById(productId);

            // 删除商品图片
            productImageService.deleteProductImages(productId);

            // 删除商品评价
            productReviewMapper.deleteByProductId(productId);

            // 删除商品
            productMapper.deleteById(productId);

            // 清除缓存
            if (product != null) {
                clearProductCache(productId, product.getCategoryId());
            }
        }

        log.info("商品删除成功: productIds={}", productIds);
    }

    @Override
    public ProductDetailResponse getProductDetail(Long productId) {
        String cacheKey = RedisConstants.getProductInfoKey(productId);
        
        // 1. 尝试从Redis获取缓存
        ProductDetailResponse cached = redisCacheService.get(cacheKey, ProductDetailResponse.class);
        if (cached != null) {
            // 检查是否是空对象（防止缓存穿透）
            if (cached.getProductId() == null) {
                log.debug("商品不存在（空对象缓存）: productId={}", productId);
                throw new BusinessException(404, "商品不存在");
            }
            log.debug("商品详情缓存命中: productId={}", productId);
            return cached;
        }

        // 2. 缓存未命中，使用分布式锁防止缓存击穿
        String lockKey = RedisConstants.getCacheLoadLockKey("product", productId);
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试获取锁，等待3秒，持有10秒
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    // 双重检查：获取锁后再次查询缓存
                    cached = redisCacheService.get(cacheKey, ProductDetailResponse.class);
                    if (cached != null) {
                        if (cached.getProductId() == null) {
                            throw new BusinessException(404, "商品不存在");
                        }
                        return cached;
                    }
                    
                    // 3. 从数据库查询
                    Product product = productMapper.selectProductDetail(productId);
                    if (product == null) {
                        // 缓存空对象，防止缓存穿透（5分钟TTL）
                        ProductDetailResponse emptyResponse = new ProductDetailResponse();
                        redisCacheService.set(cacheKey, emptyResponse, 
                                RedisConstants.EMPTY_CACHE_TTL, TimeUnit.SECONDS);
                        log.info("商品不存在，已缓存空对象: productId={}", productId);
                        throw new BusinessException(404, "商品不存在");
                    }

                    // 公开接口只返回审核通过的商品
                    if (!AuditStatus.APPROVED.getCode().equals(product.getAuditStatus())) {
                        ProductDetailResponse emptyResponse = new ProductDetailResponse();
                        redisCacheService.set(cacheKey, emptyResponse,
                                RedisConstants.EMPTY_CACHE_TTL, TimeUnit.SECONDS);
                        log.info("商品未通过审核，对普通用户不可见: productId={}, auditStatus={}", productId, product.getAuditStatus());
                        throw new BusinessException(404, "商品不存在");
                    }
                    // 4. 构建响应对象
                    ProductDetailResponse response = new ProductDetailResponse();
                    BeanUtils.copyProperties(product, response);

                    // 转换主图为完整URL
                    if (response.getMainImage() != null && !response.getMainImage().isEmpty()) {
                        response.setMainImage(fileUploadHelper.toFullUrl(response.getMainImage()));
                    }

                    // 获取商品图片（也可以缓存）
                    String imagesKey = RedisConstants.getProductImagesKey(productId);
                    List<ProductImage> images = redisCacheService.get(imagesKey, List.class);

                    if (images == null) {
                        images = productImageMapper.selectByProductId(productId);
                        // 缓存图片列表（带随机TTL）
                        int imagesTtl = RedisConstants.getRandomTtl(RedisConstants.PRODUCT_IMAGES_TTL);
                        redisCacheService.set(imagesKey, images, imagesTtl, TimeUnit.SECONDS);
                    }

                    List<ProductDetailResponse.ProductImageInfo> imageInfos = images.stream()
                            .map(image -> {
                                ProductDetailResponse.ProductImageInfo imageInfo = new ProductDetailResponse.ProductImageInfo();
                                if (image instanceof ProductImage) {
                                    ProductImage pi = (ProductImage) image;
                                    imageInfo.setImageId(pi.getImageId());
                                    imageInfo.setUrl(fileUploadHelper.toFullUrl(pi.getImageUrl()));
                                    imageInfo.setIsMain(pi.getIsMain());
                                    imageInfo.setSortOrder(pi.getSortOrder());
                                }
                                return imageInfo;
                            })
                            .collect(java.util.stream.Collectors.toList());
                    response.setImages(imageInfos);

                    // 获取评价信息
                    response.setAvgRating(productReviewMapper.selectAvgRatingByProductId(productId));
                    response.setReviewCount(productReviewMapper.selectReviewCountByProductId(productId));

                    // 5. 缓存商品详情（带随机TTL，防止缓存雪崩）
                    int ttl = RedisConstants.getRandomTtl(RedisConstants.PRODUCT_INFO_TTL);
                    redisCacheService.set(cacheKey, response, ttl, TimeUnit.SECONDS);
                    log.debug("商品详情已缓存: productId={}, ttl={}秒", productId, ttl);

                    return response;
                    
                } finally {
                    lock.unlock();
                }
            } else {
                // 获取锁失败，等待一下再查缓存
                Thread.sleep(50);
                cached = redisCacheService.get(cacheKey, ProductDetailResponse.class);
                if (cached != null && cached.getProductId() != null) {
                    return cached;
                }
                throw new BusinessException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取商品详情被中断: productId={}", productId, e);
            throw new BusinessException("系统繁忙，请稍后重试");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取商品详情异常: productId={}", productId, e);
            throw new BusinessException("获取商品详情失败");
        }
    }

    @Override
    public ProductDetailResponse getProductDetailUnfiltered(Long productId) {
        // 管理端/审核端：直接查询数据库，不过滤审核状态，不走公开缓存
        Product product = productMapper.selectProductDetail(productId);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }

        ProductDetailResponse response = new ProductDetailResponse();
        BeanUtils.copyProperties(product, response);

        if (response.getMainImage() != null && !response.getMainImage().isEmpty()) {
            response.setMainImage(fileUploadHelper.toFullUrl(response.getMainImage()));
        }

        List<ProductImage> images = productImageMapper.selectByProductId(productId);
        List<ProductDetailResponse.ProductImageInfo> imageInfos = images.stream()
                .map(image -> {
                    ProductDetailResponse.ProductImageInfo imageInfo = new ProductDetailResponse.ProductImageInfo();
                    imageInfo.setImageId(image.getImageId());
                    imageInfo.setUrl(fileUploadHelper.toFullUrl(image.getImageUrl()));
                    imageInfo.setIsMain(image.getIsMain());
                    imageInfo.setSortOrder(image.getSortOrder());
                    return imageInfo;
                })
                .collect(java.util.stream.Collectors.toList());
        response.setImages(imageInfos);

        response.setAvgRating(productReviewMapper.selectAvgRatingByProductId(productId));
        response.setReviewCount(productReviewMapper.selectReviewCountByProductId(productId));

        return response;
    }

    @Override
    public IPage<Product> getProductPage(int page, int size, Long categoryId,
                                       BigDecimal minPrice, BigDecimal maxPrice, Integer status, String sort) {
        Page<Product> pageParam = new Page<>(page, size);
        return productMapper.selectProductPage(pageParam, categoryId, minPrice, maxPrice, status, sort,
                AuditStatus.APPROVED.getCode());
    }

    @Override
    public IPage<ProductListItemResponse> getProductListWithImages(int page, int size, Long categoryId,
                                       BigDecimal minPrice, BigDecimal maxPrice, Integer status, String sort) {
        // 获取商品分页数据（公开接口只返回审核通过的商品）
        Page<Product> pageParam = new Page<>(page, size);
        IPage<Product> productPage = productMapper.selectProductPage(pageParam, categoryId, minPrice, maxPrice, status, sort,
                AuditStatus.APPROVED.getCode());

        // 转换为 ProductListItemResponse
        Page<ProductListItemResponse> responsePage = new Page<>(page, size);
        responsePage.setTotal(productPage.getTotal());
        responsePage.setPages(productPage.getPages());

        List<ProductListItemResponse> responseList = productPage.getRecords().stream().map(product -> {
            ProductListItemResponse response = new ProductListItemResponse();
            BeanUtils.copyProperties(product, response);

            // 获取商品图片列表
            List<ProductImage> images = productImageMapper.selectByProductId(product.getProductId());
            List<ProductListItemResponse.ProductImageInfo> imageInfoList = images.stream().map(image -> {
                ProductListItemResponse.ProductImageInfo imageInfo = new ProductListItemResponse.ProductImageInfo();
                imageInfo.setImageId(image.getImageId());
                // 转换为完整URL
                imageInfo.setUrl(fileUploadHelper.toFullUrl(image.getImageUrl()));
                imageInfo.setMain(image.getIsMain() == 1);
                imageInfo.setSortOrder(image.getSortOrder());
                return imageInfo;
            }).toList();
            response.setImages(imageInfoList);

            // 如果 mainImage 为空，从图片列表中获取主图
            if ((response.getMainImage() == null || response.getMainImage().isEmpty()) && !imageInfoList.isEmpty()) {
                // 优先使用标记为主图的图片
                imageInfoList.stream()
                    .filter(ProductListItemResponse.ProductImageInfo::getMain)
                    .findFirst()
                    .ifPresentOrElse(
                        img -> response.setMainImage(img.getUrl()),
                        () -> response.setMainImage(imageInfoList.get(0).getUrl())
                    );
            } else if (response.getMainImage() != null && !response.getMainImage().isEmpty()) {
                // 转换主图为完整URL
                response.setMainImage(fileUploadHelper.toFullUrl(response.getMainImage()));
            }

            // 查询评分
            response.setAvgRating(productReviewMapper.selectAvgRatingByProductId(product.getProductId()));
            response.setReviewCount(productReviewMapper.selectReviewCountByProductId(product.getProductId()));

            return response;
        }).toList();

        responsePage.setRecords(responseList);
        return responsePage;
    }

    @Override
    public IPage<ProductListItemResponse> getProductListWithImagesAdmin(int page, int size, Long categoryId,
                                       BigDecimal minPrice, BigDecimal maxPrice, Integer status, String sort,
                                       Integer auditStatus) {
        // 管理端：支持按 auditStatus 筛选，传 null 则返回所有商品
        Page<Product> pageParam = new Page<>(page, size);
        IPage<Product> productPage = productMapper.selectProductPage(pageParam, categoryId, minPrice, maxPrice, status, sort, auditStatus);

        Page<ProductListItemResponse> responsePage = new Page<>(page, size);
        responsePage.setTotal(productPage.getTotal());
        responsePage.setPages(productPage.getPages());

        List<ProductListItemResponse> responseList = productPage.getRecords().stream().map(product -> {
            ProductListItemResponse response = new ProductListItemResponse();
            BeanUtils.copyProperties(product, response);

            List<ProductImage> images = productImageMapper.selectByProductId(product.getProductId());
            List<ProductListItemResponse.ProductImageInfo> imageInfoList = images.stream().map(image -> {
                ProductListItemResponse.ProductImageInfo imageInfo = new ProductListItemResponse.ProductImageInfo();
                imageInfo.setImageId(image.getImageId());
                imageInfo.setUrl(fileUploadHelper.toFullUrl(image.getImageUrl()));
                imageInfo.setMain(image.getIsMain() == 1);
                imageInfo.setSortOrder(image.getSortOrder());
                return imageInfo;
            }).toList();
            response.setImages(imageInfoList);

            if ((response.getMainImage() == null || response.getMainImage().isEmpty()) && !imageInfoList.isEmpty()) {
                imageInfoList.stream()
                    .filter(ProductListItemResponse.ProductImageInfo::getMain)
                    .findFirst()
                    .ifPresentOrElse(
                        img -> response.setMainImage(img.getUrl()),
                        () -> response.setMainImage(imageInfoList.get(0).getUrl())
                    );
            } else if (response.getMainImage() != null && !response.getMainImage().isEmpty()) {
                response.setMainImage(fileUploadHelper.toFullUrl(response.getMainImage()));
            }

            response.setAvgRating(productReviewMapper.selectAvgRatingByProductId(product.getProductId()));
            response.setReviewCount(productReviewMapper.selectReviewCountByProductId(product.getProductId()));

            return response;
        }).toList();

        responsePage.setRecords(responseList);
        return responsePage;
    }

    @Override
    public IPage<Product> searchProducts(int page, int size, String keyword) {
        Page<Product> pageParam = new Page<>(page, size);
        return productMapper.searchProducts(pageParam, keyword, ProductStatus.ON_SHELF.getCode());
    }

    @Override
    public IPage<ProductListItemResponse> searchProductsWithFilters(int page, int size, String keyword,
                                                                    BigDecimal minPrice, BigDecimal maxPrice, String sortType) {
        Page<Product> pageParam = new Page<>(page, size);
        // 公开搜索：只返回已上架且审核通过的商品
        IPage<Product> productPage = productMapper.searchProductsWithFilters(
                pageParam, keyword, minPrice, maxPrice, ProductStatus.ON_SHELF.getCode(), sortType,
                AuditStatus.APPROVED.getCode());
        
        // 转换为响应对象，包含图片信息
        Page<ProductListItemResponse> responsePage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        List<ProductListItemResponse> responseList = productPage.getRecords().stream().map(product -> {
            ProductListItemResponse response = new ProductListItemResponse();
            response.setProductId(product.getProductId());
            response.setCategoryId(product.getCategoryId());
            response.setCategoryName(product.getCategoryName());
            response.setName(product.getName());
            response.setPrice(product.getPrice());
            response.setOriginalPrice(product.getOriginalPrice());
            response.setStock(product.getStock());
            response.setSales(product.getSales());
            response.setStatus(product.getStatus());
            response.setCreatedAt(product.getCreatedAt());
            response.setVersion(product.getVersion());

            // 获取商品图片
            List<ProductImage> images = productImageMapper.selectByProductId(product.getProductId());
            List<ProductListItemResponse.ProductImageInfo> imageInfoList = images.stream().map(img -> {
                ProductListItemResponse.ProductImageInfo imgInfo = new ProductListItemResponse.ProductImageInfo();
                imgInfo.setImageId(img.getImageId());
                imgInfo.setUrl(fileUploadHelper.toFullUrl(img.getImageUrl()));
                imgInfo.setMain(img.getIsMain() == 1);
                imgInfo.setSortOrder(img.getSortOrder());
                return imgInfo;
            }).toList();
            response.setImages(imageInfoList);

            // 设置主图
            if (product.getMainImage() != null && !product.getMainImage().isEmpty()) {
                response.setMainImage(fileUploadHelper.toFullUrl(product.getMainImage()));
            } else if (!images.isEmpty()) {
                ProductImage mainImg = images.stream()
                        .filter(img -> img.getIsMain() == 1)
                        .findFirst()
                        .orElse(images.get(0));
                response.setMainImage(fileUploadHelper.toFullUrl(mainImg.getImageUrl()));
            }

            return response;
        }).toList();

        responsePage.setRecords(responseList);
        return responsePage;
    }

    @Override
    public IPage<ProductListItemResponse> searchProductsWithFiltersAdmin(int page, int size, String keyword,
                                                                         BigDecimal minPrice, BigDecimal maxPrice,
                                                                         String sortType, Integer auditStatus) {
        Page<Product> pageParam = new Page<>(page, size);
        // 管理端支持按 auditStatus 筛选，传 null 则查询所有商品
        IPage<Product> productPage = productMapper.searchProductsWithFilters(
                pageParam, keyword, minPrice, maxPrice, null, sortType, auditStatus);
        
        // 转换为响应对象，包含图片信息
        Page<ProductListItemResponse> responsePage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        List<ProductListItemResponse> responseList = productPage.getRecords().stream().map(product -> {
            ProductListItemResponse response = new ProductListItemResponse();
            response.setProductId(product.getProductId());
            response.setCategoryId(product.getCategoryId());
            response.setCategoryName(product.getCategoryName());
            response.setName(product.getName());
            response.setPrice(product.getPrice());
            response.setOriginalPrice(product.getOriginalPrice());
            response.setStock(product.getStock());
            response.setSales(product.getSales());
            response.setStatus(product.getStatus());
            response.setAuditStatus(product.getAuditStatus());
            response.setRejectReason(product.getRejectReason());
            response.setCreatedAt(product.getCreatedAt());
            response.setVersion(product.getVersion());

            // 获取商品图片
            List<ProductImage> images = productImageMapper.selectByProductId(product.getProductId());
            List<ProductListItemResponse.ProductImageInfo> imageInfoList = images.stream().map(img -> {
                ProductListItemResponse.ProductImageInfo imgInfo = new ProductListItemResponse.ProductImageInfo();
                imgInfo.setImageId(img.getImageId());
                imgInfo.setUrl(fileUploadHelper.toFullUrl(img.getImageUrl()));
                imgInfo.setMain(img.getIsMain() == 1);
                imgInfo.setSortOrder(img.getSortOrder());
                return imgInfo;
            }).toList();
            response.setImages(imageInfoList);

            // 设置主图
            if (product.getMainImage() != null && !product.getMainImage().isEmpty()) {
                response.setMainImage(fileUploadHelper.toFullUrl(product.getMainImage()));
            } else if (!images.isEmpty()) {
                ProductImage mainImg = images.stream()
                        .filter(img -> img.getIsMain() == 1)
                        .findFirst()
                        .orElse(images.get(0));
                response.setMainImage(fileUploadHelper.toFullUrl(mainImg.getImageUrl()));
            }

            // 查询评分
            response.setAvgRating(productReviewMapper.selectAvgRatingByProductId(product.getProductId()));
            response.setReviewCount(productReviewMapper.selectReviewCountByProductId(product.getProductId()));

            return response;
        }).toList();

        responsePage.setRecords(responseList);
        return responsePage;
    }

    @Override
    @Transactional
    public void updateSales(Long productId, Integer quantity) {
        int result = productMapper.updateSales(productId, quantity);
        if (result <= 0) {
            throw new BusinessException("销量更新失败");
        }
    }

    @Override
    @Transactional
    public void updateStock(Long productId, Integer quantity) {
        // 使用StockService扣减库存（同时更新MySQL和Redis）
        if (!stockService.deductStock(productId, quantity)) {
            throw new BusinessException("库存更新失败");
        }
    }

    @Override
    public boolean checkProductPermission(Long productId, Long userId, String role) {
        // 只有管理员有权限操作商品
        return UserRole.ADMIN.getRoleName().equals(role);
    }

    /**
     * 获取当前用户角色
     */
    private String getCurrentUserRole() {
        return SecurityUtil.getCurrentUserRole();
    }
}
