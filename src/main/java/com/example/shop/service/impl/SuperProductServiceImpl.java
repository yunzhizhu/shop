package com.example.shop.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.dto.AuditRequest;
import com.example.shop.dto.ProductDetailResponse;
import com.example.shop.dto.ProductListItemResponse;
import com.example.shop.entity.Product;
import com.example.shop.enums.AuditStatus;
import com.example.shop.enums.ProductStatus;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.service.ProductService;
import com.example.shop.service.SuperProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Super 审核员商品服务实现
 */
@Slf4j
@Service
public class SuperProductServiceImpl implements SuperProductService {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 查询审核列表（分页 + 状态筛选）
     * 需求：2.1, 2.2
     */
    @Override
    public IPage<ProductListItemResponse> getAuditList(int page, int size, Integer auditStatus) {
        // 复用管理端查询逻辑，不过滤 status，支持按 auditStatus 筛选
        return productService.getProductListWithImagesAdmin(
                page, size, null, null, null, null, null, auditStatus);
    }

    /**
     * 查询商品详情（不过滤审核状态）
     * 需求：2.3, 2.4
     */
    @Override
    public ProductDetailResponse getProductDetail(Long productId) {
        return productService.getProductDetailUnfiltered(productId);
    }

    /**
     * 执行审核操作
     * APPROVE：audit_status=APPROVED, status=1（上架）
     * REJECT ：audit_status=REJECTED, status=0（下架），存储拒绝原因
     * 非 PENDING 商品返回错误
     * 需求：3.1, 3.2, 3.3
     */
    @Override
    @Transactional
    public void auditProduct(AuditRequest request) {
        if (request.getProductId() == null) {
            throw new BusinessException(400, "商品ID不能为空");
        }
        if (request.getAction() == null || request.getAction().isBlank()) {
            throw new BusinessException(400, "审核操作不能为空");
        }

        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }

        // 需求 3.3：仅允许对 PENDING 状态商品执行审核
        if (!AuditStatus.PENDING.getCode().equals(product.getAuditStatus())) {
            throw new BusinessException(400, "仅可对待审核商品执行审核操作");
        }

        String action = request.getAction().toUpperCase();

        if ("APPROVE".equals(action)) {
            // 需求 3.1：通过 → APPROVED + 上架
            product.setAuditStatus(AuditStatus.APPROVED.getCode());
            product.setStatus(ProductStatus.ON_SHELF.getCode());
            product.setRejectReason(null);
            log.info("商品审核通过: productId={}", request.getProductId());

        } else if ("REJECT".equals(action)) {
            // 需求 3.2：拒绝时必须填写原因
            if (request.getRejectReason() == null || request.getRejectReason().isBlank()) {
                throw new BusinessException(400, "审核拒绝时必须填写拒绝原因");
            }
            product.setAuditStatus(AuditStatus.REJECTED.getCode());
            product.setStatus(ProductStatus.OFF_SHELF.getCode());
            product.setRejectReason(request.getRejectReason());
            log.info("商品审核拒绝: productId={}, reason={}", request.getProductId(), request.getRejectReason());

        } else {
            throw new BusinessException(400, "无效的审核操作，仅支持 APPROVE 或 REJECT");
        }

        int result = productMapper.updateById(product);
        if (result <= 0) {
            throw new BusinessException("审核操作失败");
        }
    }
}
