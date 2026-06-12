package com.example.shop.service.impl;

import com.example.shop.constants.RedisConstants;
import com.example.shop.service.OrderNoGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 订单号生成服务实现类
 * 使用Redis INCR命令生成全局唯一的订单号
 */
@Slf4j
@Service
public class OrderNoGeneratorServiceImpl implements OrderNoGeneratorService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public String generateOrderNo() {
        try {
            // 1. 获取当前日期和时间
            LocalDateTime now = LocalDateTime.now();
            String dateStr = now.format(DATE_FORMATTER);
            String datetimeStr = now.format(DATETIME_FORMATTER);

            // 2. 构建Redis键：order:seq:20260223
            String redisKey = RedisConstants.ORDER_SEQ_KEY + dateStr;

            // 3. 使用Redis INCR命令获取序列号（原子操作，线程安全）
            Long sequence = redisTemplate.opsForValue().increment(redisKey);
            
            if (sequence == null) {
                log.error("Redis INCR返回null，使用默认序列号1");
                sequence = 1L;
            }

            // 4. 第一次使用时设置过期时间（第二天凌晨过期）
            if (sequence == 1L) {
                // 设置过期时间为2天（防止跨天问题）
                redisTemplate.expire(redisKey, 2, TimeUnit.DAYS);
                log.info("初始化订单序列号Redis键: {}", redisKey);
            }

            // 5. 格式化序列号为6位（不足补0）
            String seqStr = String.format("%06d", sequence % 1000000);

            // 6. 拼接订单号：NO + 时间戳 + 序列号
            String orderNo = "NO" + datetimeStr + seqStr;

            log.debug("生成订单号: {}, 序列号: {}", orderNo, sequence);
            return orderNo;

        } catch (Exception e) {
            log.error("生成订单号失败，使用降级方案", e);
            // 降级方案：使用时间戳 + 随机数
            String datetimeStr = LocalDateTime.now().format(DATETIME_FORMATTER);
            String randomStr = String.format("%06d", (int) (Math.random() * 1000000));
            return "NO" + datetimeStr + randomStr;
        }
    }
}
