package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shop.entity.Category;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品分类Mapper接口
 */
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 查询所有启用的分类
     */
    List<Category> selectAllEnabled();

    /**
     * 查询所有分类（包括禁用的，管理员使用）
     */
    List<Category> selectAll();

    /**
     * 根据父分类ID查询子分类
     */
    List<Category> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 检查分类下是否有商品
     */
    Long countProductsByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 检查分类下是否有子分类
     */
    Long countChildrenByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 获取分类树
     */
    List<Category> selectCategoryTree();
}
