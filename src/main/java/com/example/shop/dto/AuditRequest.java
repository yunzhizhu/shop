package com.example.shop.dto;

import lombok.Data;

/**
 * 商品审核请求DTO
 */
@Data
public class AuditRequest {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 审核操作: "APPROVE" 或 "REJECT"
     */
    private String action;

    /**
     * 拒绝原因（action为REJECT时必填）
     */
    private String rejectReason;
}
