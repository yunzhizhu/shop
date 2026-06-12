package com.example.shop.dto;

import lombok.Data;

/**
 * 批量发送结果DTO
 */
@Data
public class BatchSendResult {

    /**
     * 成功发送数量
     */
    private Integer successCount;

    /**
     * 失败发送数量
     */
    private Integer failedCount;

    /**
     * 总发送数量
     */
    private Integer totalCount;

    /**
     * 发送详情信息
     */
    private String message;

    public BatchSendResult() {
        this.successCount = 0;
        this.failedCount = 0;
        this.totalCount = 0;
    }

    public BatchSendResult(Integer successCount, Integer failedCount) {
        this.successCount = successCount != null ? successCount : 0;
        this.failedCount = failedCount != null ? failedCount : 0;
        this.totalCount = this.successCount + this.failedCount;
    }
}