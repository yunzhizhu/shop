package com.example.shop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.dto.*;
import com.example.shop.entity.Product;
import com.example.shop.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员商品管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/product")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    @Autowired
    private ProductService productService;

    /**
     * 创建商品 (支持文件上传)
     */
    @PostMapping("/create")
    @SystemLog(operation = "创建商品", module = "商品模块", action = "createProduct")
    public Result<Map<String, Object>> createProduct(
            @Valid @ModelAttribute ProductCreateRequest request,
            @RequestParam(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {

        log.info("管理员创建商品: {}", request);
        Long productId = productService.createProduct(request, mainImage, images);

        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);

        return Result.success("创建成功", data);
    }

    /**
     * 更新商品信息
     */
    @PutMapping("/update")
    @SystemLog(operation = "更新商品信息", module = "商品模块", action = "updateProduct")
    public Result<Map<String, Object>> updateProduct(@Valid @RequestBody ProductUpdateRequest request) {
        Integer newVersion = productService.updateProduct(request);
        Map<String, Object> data = new HashMap<>();
        data.put("version", newVersion);
        return Result.success("更新成功", data);
    }

    /**
     * 商品上架/下架
     */
    @PutMapping("/status")
    @SystemLog(operation = "更新商品状态", module = "商品模块", action = "updateProductStatus")
    public Result<Void> updateProductStatus(@Valid @RequestBody ProductStatusRequest request) {
        productService.updateProductStatus(request);
        return Result.success("操作成功", null);
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/delete")
    @SystemLog(operation = "删除商品", module = "商品模块", action = "deleteProducts")
    public Result<Void> deleteProducts(@RequestBody Map<String, List<Long>> request) {
        List<Long> productIds = request.get("productIds");
        productService.deleteProducts(productIds);
        return Result.success("删除成功", null);
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/detail/{productId}")
    public Result<ProductDetailResponse> getProductDetail(@PathVariable Long productId) {
        ProductDetailResponse detail = productService.getProductDetailUnfiltered(productId);
        return Result.success(detail);
    }

    /**
     * 获取商品列表(分页)
     */
    @GetMapping("/list")
    public Result<IPage<ProductListItemResponse>> getProductList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String sortType) {
        
        IPage<ProductListItemResponse> productPage = productService.getProductListWithImagesAdmin(
                page, size, categoryId, minPrice, maxPrice, status, sortType, auditStatus);
        return Result.success(productPage);
    }

    /**
     * 搜索商品
     */
    @GetMapping("/search")
    public Result<IPage<ProductListItemResponse>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String sortType) {
        
        IPage<ProductListItemResponse> productPage = productService.searchProductsWithFiltersAdmin(
                page, size, keyword, minPrice, maxPrice, sortType, auditStatus);
        return Result.success(productPage);
    }
}
