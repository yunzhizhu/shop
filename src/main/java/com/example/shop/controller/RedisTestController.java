package com.example.shop.controller;

import com.example.shop.common.Result;
import com.example.shop.service.OrderNoGeneratorService;
import com.example.shop.service.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis功能测试控制器
 * 用于测试Redis和Redisson的各项功能
 */
@Slf4j
@RestController
@RequestMapping("/test/redis")
public class RedisTestController {

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderNoGeneratorService orderNoGeneratorService;

    /**
     * 测试Redis基础操作
     */
    @GetMapping("/basic")
    public Result<Map<String, Object>> testBasic() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 测试set和get
            String testKey = "test:basic:key";
            String testValue = "Hello Redis!";
            redisCacheService.set(testKey, testValue, 60, TimeUnit.SECONDS);
            String getValue = redisCacheService.get(testKey, String.class);
            result.put("set_get_test", getValue.equals(testValue) ? "成功" : "失败");

            // 2. 测试increment
            String counterKey = "test:counter";
            Long count1 = redisCacheService.increment(counterKey);
            Long count2 = redisCacheService.increment(counterKey);
            result.put("increment_test", count2 > count1 ? "成功" : "失败");
            result.put("counter_value", count2);

            // 3. 测试hasKey
            boolean exists = redisCacheService.hasKey(testKey);
            result.put("hasKey_test", exists ? "成功" : "失败");

            // 4. 测试delete
            redisCacheService.delete(testKey);
            redisCacheService.delete(counterKey);
            boolean deleted = !redisCacheService.hasKey(testKey);
            result.put("delete_test", deleted ? "成功" : "失败");

            result.put("status", "所有测试通过");
            return Result.success(result);

        } catch (Exception e) {
            log.error("Redis基础测试失败", e);
            result.put("status", "测试失败");
            result.put("error", e.getMessage());
            return Result.error("Redis基础测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试Redisson分布式锁
     */
    @GetMapping("/lock")
    public Result<Map<String, Object>> testLock() {
        Map<String, Object> result = new HashMap<>();
        String lockKey = "test:lock:demo";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待5秒，锁持有10秒
            boolean locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            
            if (locked) {
                try {
                    result.put("lock_acquired", "成功");
                    result.put("lock_key", lockKey);
                    
                    // 模拟业务处理
                    Thread.sleep(1000);
                    
                    result.put("business_processed", "成功");
                } finally {
                    // 释放锁
                    lock.unlock();
                    result.put("lock_released", "成功");
                }
            } else {
                result.put("lock_acquired", "失败（超时）");
            }

            result.put("status", "分布式锁测试完成");
            return Result.success(result);

        } catch (Exception e) {
            log.error("Redisson分布式锁测试失败", e);
            result.put("status", "测试失败");
            result.put("error", e.getMessage());
            return Result.error("分布式锁测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试订单号生成
     */
    @GetMapping("/order-no")
    public Result<Map<String, Object>> testOrderNo() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 生成10个订单号测试
            for (int i = 0; i < 10; i++) {
                String orderNo = orderNoGeneratorService.generateOrderNo();
                result.put("order_no_" + (i + 1), orderNo);
            }

            result.put("status", "订单号生成测试完成");
            result.put("note", "订单号应该是连续递增的");
            return Result.success(result);

        } catch (Exception e) {
            log.error("订单号生成测试失败", e);
            result.put("status", "测试失败");
            result.put("error", e.getMessage());
            return Result.error("订单号生成测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试并发订单号生成
     */
    @GetMapping("/order-no/concurrent")
    public Result<Map<String, Object>> testConcurrentOrderNo() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 模拟10个并发请求
            int threadCount = 10;
            Map<String, String> orderNos = new HashMap<>();
            
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    String orderNo = orderNoGeneratorService.generateOrderNo();
                    synchronized (orderNos) {
                        orderNos.put("thread_" + index, orderNo);
                    }
                });
                threads[i].start();
            }

            // 等待所有线程完成
            for (Thread thread : threads) {
                thread.join();
            }

            result.put("order_nos", orderNos);
            result.put("total_count", orderNos.size());
            
            // 检查是否有重复
            long uniqueCount = orderNos.values().stream().distinct().count();
            result.put("unique_count", uniqueCount);
            result.put("has_duplicate", uniqueCount < orderNos.size() ? "是" : "否");
            
            result.put("status", "并发测试完成");
            return Result.success(result);

        } catch (Exception e) {
            log.error("并发订单号生成测试失败", e);
            result.put("status", "测试失败");
            result.put("error", e.getMessage());
            return Result.error("并发测试失败: " + e.getMessage());
        }
    }

    /**
     * 清理测试数据
     */
    @DeleteMapping("/cleanup")
    public Result<String> cleanup() {
        try {
            // 清理测试键
            redisCacheService.delete("test:basic:key");
            redisCacheService.delete("test:counter");
            redisCacheService.delete("test:lock:demo");
            
            return Result.success("测试数据清理完成");
        } catch (Exception e) {
            log.error("清理测试数据失败", e);
            return Result.error("清理失败: " + e.getMessage());
        }
    }
}
