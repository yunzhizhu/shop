package com.example.shop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.common.Result;
import com.example.shop.dto.AuditRequest;
import com.example.shop.dto.ProductDetailResponse;
import com.example.shop.dto.ProductListItemResponse;
import com.example.shop.service.SuperProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Super 审核员商品管理控制器
 * 需求：2.1, 2.3, 2.5, 3.1, 3.2, 3.3, 3.4
 */
@Slf4j
@RestController
@RequestMapping("/super/product")
@PreAuthorize("hasRole('SUPER')")
public class SuperProductController {

    @Autowired
    private SuperProductService superProductService;

    /**
     * 审核列表（分页 + 状态筛选）
     * 需求：2.1, 2.2
     */
    @GetMapping("/list")
    public Result<IPage<ProductListItemResponse>> getAuditList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer auditStatus) {

        IPage<ProductListItemResponse> result = superProductService.getAuditList(page, size, auditStatus);
        return Result.success(result);
    }

    /**
     * 商品详情（含所有图片、拒绝原因等完整信息）
     * 需求：2.3, 2.4
     */
    @GetMapping("/{productId}")
    public Result<ProductDetailResponse> getProductDetail(@PathVariable Long productId) {
        ProductDetailResponse detail = superProductService.getProductDetail(productId);
        return Result.success(detail);
    }

    /**
     * 审核操作（APPROVE / REJECT）
     * 需求：3.1, 3.2, 3.3
     */
    @PostMapping("/audit")
    public Result<Void> auditProduct(@RequestBody AuditRequest request) {
        superProductService.auditProduct(request);
        return Result.success("审核操作成功", null);
    }
}
