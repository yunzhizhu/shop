package com.example.shop;

import com.example.shop.mapper.CategoryMapper;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.service.CategoryService;
import com.example.shop.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ProductManagementTest {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Test
    public void testMapperLoading() {
        // 简单测试Mapper是否能正常加载
        System.out.println("ProductMapper loaded: " + (productMapper != null));
        System.out.println("CategoryMapper loaded: " + (categoryMapper != null));
        System.out.println("ProductService loaded: " + (productService != null));
        System.out.println("CategoryService loaded: " + (categoryService != null));
    }
}
