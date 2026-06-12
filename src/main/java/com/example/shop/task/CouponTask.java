package com.example.shop.task;

import com.example.shop.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 优惠券定时任务
 */
@Slf4j
@Component
public class CouponTask {

    @Autowired
    private CouponService couponService;

    /**
     * 每天凌晨1点更新过期优惠券状态
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void updateExpiredCoupons() {
        log.info("开始执行过期优惠券更新任务");
        try {
            couponService.updateExpiredCoupons();
            log.info("过期优惠券更新任务执行完成");
        } catch (Exception e) {
            log.error("过期优惠券更新任务执行失败", e);
        }
    }
}
