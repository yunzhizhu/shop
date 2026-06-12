package com.example.shop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.dto.*;
import com.example.shop.entity.Order;

import java.time.LocalDateTime;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单（从购物车）
     */
    Order createOrder(CreateOrderRequest request);

    /**
     * 立即购买（跳过购物车）
     */
    Order buyNow(BuyNowRequest request);

    /**
     * 获取订单详情
     */
    OrderDetailResponse getOrderDetail(String orderNo);

    /**
     * 获取用户订单列表
     */
    IPage<Order> getUserOrderPage(int page, int size, Integer status);

    /**
     * 获取用户订单列表（包含商品信息）
     */
    IPage<OrderListResponse> getUserOrderPageWithItems(int page, int size, Integer status);

    /**
     * 获取订单列表（管理员）
     */
    IPage<com.example.shop.dto.AdminOrderListResponse> getOrderPageForAdmin(int page, int size, Integer status, 
                                    LocalDateTime startTime, LocalDateTime endTime, String keyword);

    /**
     * 支付订单
     */
    void payOrder(OrderPayRequest request);

    /**
     * 取消订单
     */
    void cancelOrder(String orderNo);

    /**
     * 发货
     */
    void deliverOrder(String orderNo, String deliveryCompany, String trackingNo);

    /**
     * 确认收货
     */
    void receiveOrder(String orderNo);

    /**
     * 生成订单编号
     */
    String generateOrderNo();

    /**
     * 检查订单权限
     */
    boolean checkOrderPermission(String orderNo, Long userId);

    /**
     * 取消超时未支付订单
     * @return 取消的订单数量
     */
    int cancelExpiredOrders();
}
