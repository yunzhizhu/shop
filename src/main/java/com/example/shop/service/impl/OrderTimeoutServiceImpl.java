package com.example.shop.service.impl;

import com.example.shop.constants.RedisConstants;
import com.example.shop.entity.Order;
import com.example.shop.entity.OrderItem;
import com.example.shop.enums.OrderStatus;
import com.example.shop.mapper.OrderItemMapper;
import com.example.shop.mapper.OrderMapper;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.service.CouponService;
import com.example.shop.service.OrderTimeoutService;
import com.example.shop.service.StockService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 订单超时处理服务实现类
 * 使用Redisson延迟队列实现订单超时自动取消
 */
@Slf4j
@Service
public class OrderTimeoutServiceImpl implements OrderTimeoutService, CommandLineRunner {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StockService stockService;

    @Autowired
    private CouponService couponService;

    private RBlockingQueue<String> blockingQueue;
    private RDelayedQueue<String> delayedQueue;
    private ExecutorService executorService;
    private volatile boolean running = false;

    /**
     * 应用启动时自动启动消费者
     */
    @Override
    public void run(String... args) {
        startTimeoutQueueConsumer();
    }

    /**
     * 应用关闭时停止消费者
     */
    @PreDestroy
    public void destroy() {
        stopTimeoutQueueConsumer();
    }

    @Override
    public void addOrderToTimeoutQueue(String orderNo) {
        try {
            // 初始化队列（如果还没初始化）
            initQueue();

            // 添加到延迟队列，30分钟后触发
            delayedQueue.offer(orderNo, RedisConstants.ORDER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            log.info("订单已加入超时队列: orderNo={}, timeout={}ms", 
                    orderNo, RedisConstants.ORDER_TIMEOUT_MS);
        } catch (Exception e) {
            log.error("添加订单到超时队列失败: orderNo={}", orderNo, e);
        }
    }

    @Override
    public void removeOrderFromTimeoutQueue(String orderNo) {
        try {
            // 初始化队列（如果还没初始化）
            initQueue();

            // 从延迟队列中移除
            boolean removed = delayedQueue.remove(orderNo);
            
            if (removed) {
                log.info("订单已从超时队列移除: orderNo={}", orderNo);
            } else {
                log.debug("订单不在超时队列中: orderNo={}", orderNo);
            }
        } catch (Exception e) {
            log.error("从超时队列移除订单失败: orderNo={}", orderNo, e);
        }
    }

    @Override
    @Transactional
    public void handleTimeoutOrder(String orderNo) {
        try {
            log.info("开始处理超时订单: orderNo={}", orderNo);

            // 1. 获取订单
            Order order = orderMapper.selectByOrderNo(orderNo);
            if (order == null) {
                log.warn("订单不存在: orderNo={}", orderNo);
                return;
            }

            // 2. 检查订单状态
            OrderStatus orderStatus = OrderStatus.getByCode(order.getStatus());
            if (orderStatus == null || !OrderStatus.PENDING_PAYMENT.equals(orderStatus)) {
                log.info("订单状态不是待支付，无需取消: orderNo={}, status={}", 
                        orderNo, orderStatus != null ? orderStatus.getDescription() : "未知");
                return;
            }

            // 3. 更新订单状态为已取消
            int result = orderMapper.updateOrderStatus(
                orderNo, 
                OrderStatus.CANCELLED.getCode(), 
                order.getVersion()
            );
            
            if (result <= 0) {
                log.warn("订单状态更新失败（可能已被其他操作修改）: orderNo={}", orderNo);
                return;
            }

            // 4. 释放预占库存（同时更新MySQL和Redis）
            List<OrderItem> orderItems = orderItemMapper.selectByOrderNo(orderNo);
            for (OrderItem item : orderItems) {
                if (stockService.releaseReservedStock(item.getProductId(), item.getQuantity())) {
                    log.info("释放预占库存成功: orderNo={}, productId={}, quantity={}", 
                            orderNo, item.getProductId(), item.getQuantity());
                } else {
                    log.error("释放预占库存失败: orderNo={}, productId={}, quantity={}", 
                             orderNo, item.getProductId(), item.getQuantity());
                }
            }

            // 5. 退还优惠券（如果失败不影响订单取消）
            if (order.getUserCouponId() != null) {
                try {
                    // 使用内部方法，不需要用户上下文
                    ((CouponServiceImpl) couponService).returnCouponInternal(order.getUserCouponId());
                    log.info("退还优惠券成功: orderNo={}, userCouponId={}", 
                            orderNo, order.getUserCouponId());
                } catch (Exception e) {
                    // 优惠券退还失败不影响订单取消，只记录日志
                    log.error("退还优惠券失败（不影响订单取消）: orderNo={}, userCouponId={}, error={}", 
                             orderNo, order.getUserCouponId(), e.getMessage());
                }
            }

            log.info("订单超时自动取消成功: orderNo={}, userId={}, totalAmount={}", 
                    orderNo, order.getUserId(), order.getTotalAmount());

        } catch (Exception e) {
            log.error("处理超时订单失败: orderNo={}", orderNo, e);
        }
    }

    @Override
    public void startTimeoutQueueConsumer() {
        if (running) {
            log.warn("订单超时队列消费者已在运行");
            return;
        }

        try {
            // 初始化队列
            initQueue();

            // 创建线程池
            executorService = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "OrderTimeoutConsumer");
                thread.setDaemon(true);
                return thread;
            });

            // 启动消费者
            running = true;
            executorService.submit(() -> {
                log.info("订单超时队列消费者已启动");
                
                while (running) {
                    try {
                        // 阻塞等待队列中的订单号
                        String orderNo = blockingQueue.take();
                        
                        if (orderNo != null && !orderNo.isEmpty()) {
                            log.info("收到超时订单: orderNo={}", orderNo);
                            handleTimeoutOrder(orderNo);
                        }
                    } catch (InterruptedException e) {
                        if (running) {
                            log.warn("订单超时队列消费者被中断", e);
                        }
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("处理超时订单异常", e);
                        // 继续处理下一个订单
                    }
                }
                
                log.info("订单超时队列消费者已停止");
            });

            log.info("订单超时处理服务启动成功");
        } catch (Exception e) {
            log.error("启动订单超时队列消费者失败", e);
            running = false;
        }
    }

    @Override
    public void stopTimeoutQueueConsumer() {
        if (!running) {
            return;
        }

        try {
            log.info("正在停止订单超时队列消费者...");
            running = false;

            if (executorService != null) {
                executorService.shutdown();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            }

            log.info("订单超时队列消费者已停止");
        } catch (Exception e) {
            log.error("停止订单超时队列消费者失败", e);
        }
    }

    /**
     * 初始化队列
     */
    private void initQueue() {
        if (blockingQueue == null || delayedQueue == null) {
            synchronized (this) {
                if (blockingQueue == null || delayedQueue == null) {
                    blockingQueue = redissonClient.getBlockingQueue(RedisConstants.ORDER_TIMEOUT_QUEUE);
                    delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
                    log.info("订单超时队列已初始化: {}", RedisConstants.ORDER_TIMEOUT_QUEUE);
                }
            }
        }
    }
}
