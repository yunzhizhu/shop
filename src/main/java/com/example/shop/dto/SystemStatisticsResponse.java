package com.example.shop.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 系统统计响应
 */
@Data
public class SystemStatisticsResponse {
    /**
     * 总用户数
     */
    private Long userCount;
    
    /**
     * 商品数量
     */
    private Long productCount;
    
    /**
     * 订单数量
     */
    private Long orderCount;
    
    /**
     * 今日订单数
     */
    private Long todayOrderCount;
    
    /**
     * 本月订单数
     */
    private Long monthOrderCount;
    
    /**
     * 今日营收
     */
    private BigDecimal todayRevenue;
    
    /**
     * 本月营收
     */
    private BigDecimal monthRevenue;
    
    /**
     * 总营收
     */
    private BigDecimal totalRevenue;
}
