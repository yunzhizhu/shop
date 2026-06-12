package com.example.shop.controller;

import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.service.ProductImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 商品图片管理控制器 */
@Slf4j
@RestController
@RequestMapping("/product/image")
public class ProductImageController {

    @Autowired
    private ProductImageService productImageService;

    /**
     * 上传商品图片
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "上传商品图片", module = "商品模块", action = "uploadProductImage")
    public Result<Map<String, Object>> uploadProductImage(
            @RequestParam Long productId,
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "0") Integer isMain) {

        String imageUrl = productImageService.uploadProductImage(productId, file, isMain);

        Map<String, Object> data = new HashMap<>();
        data.put("imageUrl", imageUrl);

        return Result.success("上传成功", data);
    }

    /**
     * 设置主图
     */
    @PutMapping("/setMain")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "设置商品主图", module = "商品模块", action = "setMainImage")
    public Result<Void> setMainImage(@RequestBody Map<String, Long> request) {
        Long imageId = request.get("imageId");
        Long productId = request.get("productId");

        productImageService.setMainImage(imageId, productId);
        return Result.success("设置成功", null);
    }

    /**
     * 删除商品图片
     */
    @DeleteMapping("/delete/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "删除商品图片", module = "商品模块", action = "deleteProductImage")
    public Result<Void> deleteProductImage(@PathVariable Long imageId) {
        productImageService.deleteProductImage(imageId);
        return Result.success("删除成功", null);
    }
}
