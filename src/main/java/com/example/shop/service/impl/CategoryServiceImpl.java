package com.example.shop.service.impl;

import com.example.shop.constants.RedisConstants;
import com.example.shop.dto.CategoryCreateRequest;
import com.example.shop.entity.Category;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.CategoryMapper;
import com.example.shop.service.CategoryService;
import com.example.shop.service.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品分类服务实现类
 * v2.0 添加Redis缓存支持
 */
@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    @Transactional
    public Long createCategory(CategoryCreateRequest request) {
        // 检查父分类是否存在（如果不是根分类）
        if (request.getParentId() != 0) {
            Category parentCategory = categoryMapper.selectById(request.getParentId());
            if (parentCategory == null) {
                throw new BusinessException(404, "父分类不存在");
            }
        }

        // 创建分类
        Category category = new Category();
        category.setCategoryName(request.getName());  // 手动设置名称
        category.setParentId(request.getParentId());
        category.setSortOrder(request.getSortOrder());
        
        // 设置默认状态（如果未指定，默认启用）
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        } else {
            category.setStatus(1);
        }

        // 设置默认排序权重
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }

        int result = categoryMapper.insert(category);
        if (result <= 0) {
            throw new BusinessException("分类创建失败");
        }

        log.info("分类创建成功: categoryId={}, name={}", category.getCategoryId(), category.getCategoryName());
        
        // 清除分类缓存
        clearCategoryCache();
        
        return category.getCategoryId();
    }
    
    /**
     * 清除所有分类缓存
     */
    private void clearCategoryCache() {
        redisCacheService.delete(RedisConstants.CATEGORY_LIST_ALL_KEY);
        redisCacheService.delete(RedisConstants.CATEGORY_LIST_ENABLED_KEY);
        log.debug("已清除分类缓存");
    }

    @Override
    @Transactional
    public void updateCategory(Long categoryId, CategoryCreateRequest request) {
        // 检查分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }

        // 检查父分类是否存在（如果不是根分类）
        if (request.getParentId() != 0) {
            Category parentCategory = categoryMapper.selectById(request.getParentId());
            if (parentCategory == null) {
                throw new BusinessException(404, "父分类不存在");
            }

            // 不能将分类设置为自己的子分类
            if (request.getParentId().equals(categoryId)) {
                throw new BusinessException(400, "不能将分类设置为自己的子分类");
            }
        }

        // 更新分类信息
        if (request.getName() != null) {
            category.setCategoryName(request.getName());
        }
        if (request.getParentId() != null) {
            category.setParentId(request.getParentId());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        int result = categoryMapper.updateById(category);
        if (result <= 0) {
            throw new BusinessException("分类更新失败");
        }

        log.info("分类更新成功: categoryId={}", categoryId);
        
        // 清除分类缓存
        clearCategoryCache();
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        // 检查分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }

        // 检查是否可以删除
        if (!canDeleteCategory(categoryId)) {
            throw new BusinessException(400, "分类下有商品或子分类，无法删除");
        }

        // 删除分类
        int result = categoryMapper.deleteById(categoryId);
        if (result <= 0) {
            throw new BusinessException("分类删除失败");
        }

        log.info("分类删除成功: categoryId={}", categoryId);
        
        // 清除分类缓存
        clearCategoryCache();
    }

    @Override
    public List<Category> getCategoryTree() {
        // 获取所有启用的分类
        List<Category> allCategories = categoryMapper.selectAllEnabled();
        
        // 构建分类树
        return buildCategoryTree(allCategories, 0L);
    }

    @Override
    public List<Category> getAllEnabledCategories() {
        String cacheKey = RedisConstants.CATEGORY_LIST_ENABLED_KEY;
        
        // 1. 尝试从Redis获取缓存
        List<Category> cached = redisCacheService.get(cacheKey, List.class);
        if (cached != null) {
            // 检查是否是空列表（防止缓存穿透）
            if (cached.isEmpty()) {
                log.debug("分类列表为空（空对象缓存）");
                return new ArrayList<>();
            }
            log.debug("分类列表缓存命中");
            return cached;
        }
        
        // 2. 缓存未命中，从数据库查询
        List<Category> categories = categoryMapper.selectAllEnabled();
        
        // 3. 缓存到Redis（带随机TTL，防止缓存雪崩）
        int ttl = RedisConstants.getRandomTtl(RedisConstants.CATEGORY_LIST_ENABLED_TTL);
        redisCacheService.set(cacheKey, categories, ttl, TimeUnit.SECONDS);
        log.debug("分类列表已缓存: size={}, ttl={}秒", categories.size(), ttl);
        
        return categories;
    }

    @Override
    public List<Category> getAllCategories() {
        String cacheKey = RedisConstants.CATEGORY_LIST_ALL_KEY;
        
        // 1. 尝试从Redis获取缓存
        List<Category> cached = redisCacheService.get(cacheKey, List.class);
        if (cached != null) {
            // 检查是否是空列表（防止缓存穿透）
            if (cached.isEmpty()) {
                log.debug("所有分类列表为空（空对象缓存）");
                return new ArrayList<>();
            }
            log.debug("所有分类列表缓存命中");
            return cached;
        }
        
        // 2. 缓存未命中，从数据库查询
        List<Category> categories = categoryMapper.selectAll();
        
        // 3. 缓存到Redis（带随机TTL，防止缓存雪崩）
        int ttl = RedisConstants.getRandomTtl(RedisConstants.CATEGORY_LIST_ALL_TTL);
        redisCacheService.set(cacheKey, categories, ttl, TimeUnit.SECONDS);
        log.debug("所有分类列表已缓存: size={}, ttl={}秒", categories.size(), ttl);
        
        return categories;
    }

    @Override
    public List<Category> getCategoriesByParentId(Long parentId) {
        return categoryMapper.selectByParentId(parentId);
    }

    @Override
    public boolean canDeleteCategory(Long categoryId) {
        // 检查分类下是否有商品
        Long productCount = categoryMapper.countProductsByCategoryId(categoryId);
        if (productCount > 0) {
            return false;
        }

        // 检查分类下是否有子分类
        Long childrenCount = categoryMapper.countChildrenByCategoryId(categoryId);
        if (childrenCount > 0) {
            return false;
        }

        return true;
    }

    /**
     * 构建分类树
     */
    private List<Category> buildCategoryTree(List<Category> allCategories, Long parentId) {
        List<Category> result = new ArrayList<>();

        // 按父分类ID分组
        Map<Long, List<Category>> categoryMap = allCategories.stream()
                .collect(Collectors.groupingBy(Category::getParentId));

        // 获取指定父分类下的子分类
        List<Category> children = categoryMap.get(parentId);
        if (children != null) {
            for (Category category : children) {
                // 递归构建子分类树
                List<Category> subChildren = buildCategoryTree(allCategories, category.getCategoryId());
                category.setChildren(subChildren);
                result.add(category);
            }
        }

        // 按排序权重排序
        result.sort((c1, c2) -> {
            int sort1 = c1.getSortOrder() != null ? c1.getSortOrder() : 0;
            int sort2 = c2.getSortOrder() != null ? c2.getSortOrder() : 0;
            return Integer.compare(sort1, sort2); // 升序排列
        });

        return result;
    }
}
