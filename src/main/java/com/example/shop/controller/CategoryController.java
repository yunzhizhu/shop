package com.example.shop.controller;

import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.dto.CategoryCreateRequest;
import com.example.shop.entity.Category;
import com.example.shop.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品分类控制器
 */
@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 创建分类(管理员)
     */
    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "创建商品分类", module = "分类模块", action = "createCategory")
    public Result<Map<String, Object>> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        Long categoryId = categoryService.createCategory(request);
        
        Map<String, Object> data = new HashMap<>();
        data.put("categoryId", categoryId);
        
        return Result.success("创建成功", data);
    }

    /**
     * 更新分类(管理员)
     */
    @PutMapping("/admin/update/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "更新商品分类", module = "分类模块", action = "updateCategory")
    public Result<Void> updateCategory(@PathVariable Long categoryId, 
                                     @Valid @RequestBody CategoryCreateRequest request) {
        categoryService.updateCategory(categoryId, request);
        return Result.success("更新成功", null);
    }

    /**
     * 删除分类(管理员)
     */
    @DeleteMapping("/admin/delete/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "删除商品分类", module = "分类模块", action = "deleteCategory")
    public Result<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return Result.success("删除成功", null);
    }

    /**
     * 获取所有分类列表（管理员）
     * 包含所有状态的分类，用于管理后台
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Category>> getAdminCategoryList() {
        List<Category> categories = categoryService.getAllCategories();
        return Result.success(categories);
    }

    /**
     * 获取分类树
     */
    @GetMapping("/tree")
    public Result<List<Category>> getCategoryTree() {
        List<Category> categoryTree = categoryService.getCategoryTree();
        return Result.success(categoryTree);
    }

    /**
     * 获取所有启用的分类（不包含子分类）
     */
    @GetMapping("/list")
    public Result<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllEnabledCategories();
        return Result.success(categories);
    }

    /**
     * 根据父分类ID获取子分类
     */
    @GetMapping("/children/{parentId}")
    public Result<List<Category>> getCategoriesByParentId(@PathVariable Long parentId) {
        List<Category> categories = categoryService.getCategoriesByParentId(parentId);
        return Result.success(categories);
    }
}
