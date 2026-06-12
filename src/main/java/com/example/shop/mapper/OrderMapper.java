package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.entity.Order;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单Mapper接口
 */
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 根据订单编号查询订单
     */
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 分页查询用户订单列表
     */
    IPage<Order> selectUserOrderPage(Page<Order> page,
                                   @Param("userId") Long userId,
                                   @Param("status") Integer status);

    /**
     * 分页查询用户订单列表（包含商品信息）
     */
    IPage<com.example.shop.dto.OrderListResponse> selectUserOrderPageWithItems(Page<com.example.shop.dto.OrderListResponse> page,
                                                                               @Param("userId") Long userId,
                                                                               @Param("status") Integer status);

    /**
     * 分页查询订单列表（管理员）
     */
    IPage<com.example.shop.dto.AdminOrderListResponse> selectOrderPageForAdmin(Page<com.example.shop.dto.AdminOrderListResponse> page,
                                       @Param("status") Integer status,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime,
                                       @Param("keyword") String keyword);

    /**
     * 更新订单状态
     */
    int updateOrderStatus(@Param("orderNo") String orderNo,
                         @Param("status") Integer status,
                         @Param("version") Integer version);

    /**
     * 更新支付信息
     */
    int updatePaymentInfo(@Param("orderNo") String orderNo,
                         @Param("paymentType") Integer paymentType,
                         @Param("paymentTime") LocalDateTime paymentTime,
                         @Param("status") Integer status,
                         @Param("version") Integer version);

    /**
     * 更新发货信息
     */
    int updateDeliveryInfo(@Param("orderNo") String orderNo,
                          @Param("deliveryTime") LocalDateTime deliveryTime,
                          @Param("status") Integer status,
                          @Param("version") Integer version);

    /**
     * 获取订单详情（包含地址信息）
     */
    Order selectOrderDetailByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 统计指定日期的订单数量
     */
    Long countOrdersByDate(@Param("date") String date);

    /**
     * 统计日期范围内的订单数量
     */
    Long countOrdersByDateRange(@Param("startDate") String startDate, 
                                 @Param("endDate") String endDate);

    /**
     * 统计指定日期的营收
     */
    BigDecimal sumRevenueByDate(@Param("date") String date);

    /**
     * 统计日期范围内的营收
     */
    BigDecimal sumRevenueByDateRange(@Param("startDate") String startDate, 
                                      @Param("endDate") String endDate);

    /**
     * 统计总营收
     */
    BigDecimal sumTotalRevenue();

    /**
     * 查询超时未支付订单
     * @param timeoutMinutes 超时时间（分钟）
     * @return 超时订单列表
     */
    java.util.List<Order> selectExpiredOrders(@Param("timeoutMinutes") Integer timeoutMinutes);
}
