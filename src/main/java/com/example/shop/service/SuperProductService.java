package com.example.shop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.dto.AuditRequest;
import com.example.shop.dto.ProductDetailResponse;
import com.example.shop.dto.ProductListItemResponse;

/**
 * Super 审核员商品服务接口
 */
public interface SuperProductService {

    /**
     * 查询审核列表（分页 + 状态筛选）
     * 需求：2.1, 2.2
     */
    IPage<ProductListItemResponse> getAuditList(int page, int size, Integer auditStatus);

    /**
     * 查询商品详情（不过滤审核状态）
     * 需求：2.3, 2.4
     */
    ProductDetailResponse getProductDetail(Long productId);

    /**
     * 执行审核操作（APPROVE / REJECT）
     * 需求：3.1, 3.2, 3.3
     */
    void auditProduct(AuditRequest request);
}
