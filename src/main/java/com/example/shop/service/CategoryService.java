package com.example.shop.service;

import com.example.shop.dto.CategoryCreateRequest;
import com.example.shop.entity.Category;

import java.util.List;

/**
 * 商品分类服务接口
 */
public interface CategoryService {

    /**
     * 创建分类
     */
    Long createCategory(CategoryCreateRequest request);

    /**
     * 更新分类
     */
    void updateCategory(Long categoryId, CategoryCreateRequest request);

    /**
     * 删除分类
     */
    void deleteCategory(Long categoryId);

    /**
     * 获取分类树
     */
    List<Category> getCategoryTree();

    /**
     * 获取所有启用的分类
     */
    List<Category> getAllEnabledCategories();

    /**
     * 获取所有分类（包括禁用的，管理员使用）
     */
    List<Category> getAllCategories();

    /**
     * 根据父分类ID获取子分类
     */
    List<Category> getCategoriesByParentId(Long parentId);

    /**
     * 检查分类是否可以删除
     */
    boolean canDeleteCategory(Long categoryId);
}
