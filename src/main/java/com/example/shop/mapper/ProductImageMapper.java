package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shop.entity.ProductImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品图片Mapper接口
 */
public interface ProductImageMapper extends BaseMapper<ProductImage> {

    /**
     * 根据商品ID查询图片列表
     */
    List<ProductImage> selectByProductId(@Param("productId") Long productId);

    /**
     * 根据商品ID查询主图
     */
    ProductImage selectMainImageByProductId(@Param("productId") Long productId);

    /**
     * 清除商品的所有主图标记
     */
    int clearMainImageByProductId(@Param("productId") Long productId);

    /**
     * 设置主图
     */
    int setMainImage(@Param("imageId") Long imageId, @Param("productId") Long productId);

    /**
     * 根据商品ID删除所有图片
     */
    int deleteByProductId(@Param("productId") Long productId);
}
