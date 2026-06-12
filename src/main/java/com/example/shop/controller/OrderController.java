package com.example.shop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.dto.*;
import com.example.shop.entity.Order;
import com.example.shop.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单(从购物车)
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "创建订单", module = "订单模块", action = "createOrder")
    public Result<Map<String, Object>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(request);
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("orderId", order.getOrderId());
        data.put("totalAmount", order.getTotalAmount());
        data.put("paymentAmount", order.getPaymentAmount());
        
        return Result.success("创建成功", data);
    }

    /**
     * 立即购买(跳过购物车)
     */
    @PostMapping("/buy-now")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "立即购买", module = "订单模块", action = "buyNow")
    public Result<Map<String, Object>> buyNow(@Valid @RequestBody BuyNowRequest request) {
        Order order = orderService.buyNow(request);
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("orderId", order.getOrderId());
        data.put("totalAmount", order.getTotalAmount());
        data.put("paymentAmount", order.getPaymentAmount());
        
        return Result.success("订单创建成功", data);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/detail/{orderNo}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<OrderDetailResponse> getOrderDetail(@PathVariable String orderNo) {
        OrderDetailResponse detail = orderService.getOrderDetail(orderNo);
        return Result.success(detail);
    }

    /**
     * 获取订单列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Result<IPage<OrderListResponse>> getOrderList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        
        IPage<OrderListResponse> orderPage = orderService.getUserOrderPageWithItems(page, size, status);
        return Result.success(orderPage);
    }

    /**
     * 订单支付（模拟支付）
     * 
     * 注意：当前为开发测试环境，支付功能为模拟实现
     * - 支付宝支付：未集成真实SDK，直接标记订单为已支付
     * - 微信支付：未集成真实SDK，直接标记订单为已支付
     * - 余额支付：真实扣减用户余额
     * 
     * 生产环境需要集成真实的支付宝/微信支付SDK
     */
    @PostMapping("/pay")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "订单支付", module = "订单模块", action = "payOrder")
    public Result<Map<String, Object>> payOrder(@Valid @RequestBody OrderPayRequest request) {
        orderService.payOrder(request);
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderStatus", 1);
        data.put("paymentTime", LocalDateTime.now());
        
        return Result.success("支付成功", data);
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "取消订单", module = "订单模块", action = "cancelOrder")
    public Result<Map<String, Object>> cancelOrder(@RequestBody Map<String, String> request) {
        String orderNo = request.get("orderNo");
        orderService.cancelOrder(orderNo);
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderStatus", 4);
        
        return Result.success("取消成功", data);
    }

    /**
     * 确认收货
     */
    @PostMapping("/receive")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SystemLog(operation = "确认收货", module = "订单模块", action = "receiveOrder")
    public Result<Map<String, Object>> receiveOrder(@RequestBody Map<String, String> request) {
        String orderNo = request.get("orderNo");
        orderService.receiveOrder(orderNo);
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderStatus", 3);
        
        return Result.success("确认收货成功", data);
    }

    /**
     * 获取订单列表(管理员)
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<com.example.shop.dto.AdminOrderListResponse>> getOrderListForAdmin(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String keyword) {
        
        IPage<com.example.shop.dto.AdminOrderListResponse> orderPage = orderService.getOrderPageForAdmin(page, size, status, startTime, endTime, keyword);
        return Result.success(orderPage);
    }

    /**
     * 发货
     */
    @PostMapping("/admin/deliver")
    @PreAuthorize("hasRole('ADMIN')")
    @SystemLog(operation = "订单发货", module = "订单模块", action = "deliverOrder")
    public Result<Map<String, Object>> deliverOrder(@RequestBody Map<String, String> request) {
        String orderNo = request.get("orderNo");
        String deliveryCompany = request.get("deliveryCompany");
        String trackingNo = request.get("trackingNo");
        
        orderService.deliverOrder(orderNo, deliveryCompany, trackingNo);
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderStatus", 2);
        data.put("deliveryTime", LocalDateTime.now());
        
        return Result.success("发货成功", data);
    }
}
