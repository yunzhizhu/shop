package com.example.shop.service;

/**
 * 订单超时处理服务接口
 * 使用Redisson延迟队列实现订单超时自动取消
 */
public interface OrderTimeoutService {

    /**
     * 添加订单到超时队列
     * 订单创建后调用，30分钟后自动触发取消检查
     * 
     * @param orderNo 订单号
     */
    void addOrderToTimeoutQueue(String orderNo);

    /**
     * 从超时队列中移除订单
     * 订单支付成功后调用，取消超时检查
     * 
     * @param orderNo 订单号
     */
    void removeOrderFromTimeoutQueue(String orderNo);

    /**
     * 处理超时订单
     * 由延迟队列消费者调用
     * 
     * @param orderNo 订单号
     */
    void handleTimeoutOrder(String orderNo);

    /**
     * 启动延迟队列消费者
     */
    void startTimeoutQueueConsumer();

    /**
     * 停止延迟队列消费者
     */
    void stopTimeoutQueueConsumer();
}
