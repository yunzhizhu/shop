package com.example.shop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.dto.*;
import com.example.shop.entity.Product;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品服务接口
 */
public interface ProductService {

    /**
     * 创建商品
     */
    Long createProduct(ProductCreateRequest request, MultipartFile mainImage, MultipartFile[] images);

    /**
     * 更新商品信息
     * @return 更新后的版本号
     */
    Integer updateProduct(ProductUpdateRequest request);

    /**
     * 更新商品状态
     */
    void updateProductStatus(ProductStatusRequest request);

    /**
     * 删除商品
     */
    void deleteProducts(List<Long> productIds);

    /**
     * 获取商品详情
     */
    ProductDetailResponse getProductDetail(Long productId);

    /**
     * 获取商品详情（管理端/审核端，不过滤审核状态）
     */
    ProductDetailResponse getProductDetailUnfiltered(Long productId);

    /**
     * 分页查询商品列表
     */
    IPage<Product> getProductPage(int page, int size, Long categoryId,
                                 BigDecimal minPrice, BigDecimal maxPrice, Integer status, String sort);

    /**
     * 分页查询商品列表（包含图片信息）
     */
    IPage<ProductListItemResponse> getProductListWithImages(int page, int size, Long categoryId,
                                 BigDecimal minPrice, BigDecimal maxPrice, Integer status, String sort);

    /**
     * 搜索商品
     */
    IPage<Product> searchProducts(int page, int size, String keyword);

    /**
     * 搜索商品（带筛选和排序）- 用户端
     */
    IPage<ProductListItemResponse> searchProductsWithFilters(int page, int size, String keyword,
                                                             BigDecimal minPrice, BigDecimal maxPrice, String sortType);

    /**
     * 搜索商品（带筛选和排序）- 管理端
     */
    IPage<ProductListItemResponse> searchProductsWithFiltersAdmin(int page, int size, String keyword,
                                                                  BigDecimal minPrice, BigDecimal maxPrice,
                                                                  String sortType, Integer auditStatus);

    /**
     * 分页查询商品列表（包含图片信息）- 管理端，不过滤审核状态
     */
    IPage<ProductListItemResponse> getProductListWithImagesAdmin(int page, int size, Long categoryId,
                                 BigDecimal minPrice, BigDecimal maxPrice, Integer status, String sort,
                                 Integer auditStatus);

    /**
     * 更新商品销量
     */
    void updateSales(Long productId, Integer quantity);

    /**
     * 更新商品库存
     */
    void updateStock(Long productId, Integer quantity);

    /**
     * 检查商品权限
     */
    boolean checkProductPermission(Long productId, Long userId, String role);
}
