package com.example.shop.task;

import com.example.shop.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单定时任务
 * 注意：已使用Redisson延迟队列替代，此定时任务作为备份保留
 */
@Slf4j
@Component
public class OrderTask {

    @Autowired
    private OrderService orderService;

    /**
     * 每5分钟扫描一次超时未支付订单并自动取消
     * 订单创建后30分钟未支付将被自动取消
     * 
     * 注意：此定时任务已被Redisson延迟队列替代，作为备份保留
     * 如需启用，请取消注释@Scheduled注解
     */
    // @Scheduled(cron = "0 */5 * * * ?")
    public void cancelExpiredOrders() {
        log.info("开始执行订单超时取消任务（备份方案）");
        try {
            int cancelledCount = orderService.cancelExpiredOrders();
            if (cancelledCount > 0) {
                log.info("订单超时取消任务执行完成，共取消 {} 个订单", cancelledCount);
            } else {
                log.debug("订单超时取消任务执行完成，无超时订单");
            }
        } catch (Exception e) {
            log.error("订单超时取消任务执行失败", e);
        }
    }
}
