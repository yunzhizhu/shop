package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.entity.Product;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 商品Mapper接口
 */
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 分页查询商品列表
     */
    IPage<Product> selectProductPage(Page<Product> page,
                                   @Param("categoryId") Long categoryId,
                                   @Param("minPrice") BigDecimal minPrice,
                                   @Param("maxPrice") BigDecimal maxPrice,
                                   @Param("status") Integer status,
                                   @Param("sort") String sort,
                                   @Param("auditStatus") Integer auditStatus);

    /**
     * 搜索商品
     */
    IPage<Product> searchProducts(Page<Product> page,
                                @Param("keyword") String keyword,
                                @Param("status") Integer status);

    /**
     * 搜索商品（带筛选和排序）
     */
    IPage<Product> searchProductsWithFilters(Page<Product> page,
                                            @Param("keyword") String keyword,
                                            @Param("minPrice") BigDecimal minPrice,
                                            @Param("maxPrice") BigDecimal maxPrice,
                                            @Param("status") Integer status,
                                            @Param("sort") String sort,
                                            @Param("auditStatus") Integer auditStatus);

    /**
     * 更新商品销量
     */
    int updateSales(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 更新商品库存
     */
    int updateStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 预占库存
     */
    int reserveStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 释放预占库存
     */
    int releaseReservedStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 获取可用库存
     */
    Integer getAvailableStock(@Param("productId") Long productId);

    /**
     * 获取商品详情
     */
    Product selectProductDetail(@Param("productId") Long productId);

    /**
     * 更新商品主图
     */
    int updateMainImage(@Param("productId") Long productId, @Param("mainImage") String mainImage);
}
