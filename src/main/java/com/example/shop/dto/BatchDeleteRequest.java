package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量删除请求DTO
 */
@Data
public class BatchDeleteRequest {

    /**
     * 用户ID列表
     */
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;
}
