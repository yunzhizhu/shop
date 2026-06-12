package com.example.shop.service.impl;

import com.example.shop.constants.RedisConstants;
import com.example.shop.entity.Product;
import com.example.shop.mapper.CartMapper;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.service.RedisCacheService;
import com.example.shop.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 库存管理服务实现类
 * v2.0 使用Redis缓存 + 分布式锁 + 双写模式
 */
@Slf4j
@Service
public class StockServiceImpl implements StockService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 预占库存（双写模式：Redis + MySQL）
     * 使用分布式锁保证原子性
     */
    @Override
    @Transactional
    public boolean reserveStock(Long productId, Integer quantity) {
        String lockKey = RedisConstants.getStockLockKey(productId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取分布式锁，等待5秒，持有10秒
            if (lock.tryLock(RedisConstants.LOCK_WAIT_TIME, RedisConstants.LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    // 1. 检查Redis中的可用库存
                    Integer availableStock = getAvailableStockFromRedis(productId);
                    if (availableStock < quantity) {
                        log.warn("库存预占失败，库存不足: productId={}, available={}, required={}", 
                                productId, availableStock, quantity);
                        return false;
                    }

                    // 2. 更新MySQL预占库存
                    int mysqlResult = productMapper.reserveStock(productId, quantity);
                    if (mysqlResult <= 0) {
                        log.error("MySQL库存预占失败: productId={}, quantity={}", productId, quantity);
                        return false;
                    }

                    // 3. 更新Redis预占库存
                    String reservedKey = RedisConstants.getReservedStockKey(productId);
                    Long newReserved = redisCacheService.increment(reservedKey, quantity);
                    
                    log.info("库存预占成功: productId={}, quantity={}, newReserved={}", 
                            productId, quantity, newReserved);
                    return true;

                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取库存锁超时: productId={}", productId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("库存预占被中断: productId={}, quantity={}", productId, quantity, e);
            return false;
        } catch (Exception e) {
            log.error("库存预占异常: productId={}, quantity={}", productId, quantity, e);
            return false;
        }
    }

    /**
     * 释放预占库存（双写模式：Redis + MySQL）
     * 使用分布式锁保证原子性
     */
    @Override
    @Transactional
    public boolean releaseReservedStock(Long productId, Integer quantity) {
        String lockKey = RedisConstants.getStockLockKey(productId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(RedisConstants.LOCK_WAIT_TIME, RedisConstants.LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    // 1. 更新MySQL预占库存
                    int mysqlResult = productMapper.releaseReservedStock(productId, quantity);
                    if (mysqlResult <= 0) {
                        log.error("MySQL预占库存释放失败: productId={}, quantity={}", productId, quantity);
                        return false;
                    }

                    // 2. 更新Redis预占库存
                    String reservedKey = RedisConstants.getReservedStockKey(productId);
                    Long newReserved = redisCacheService.decrement(reservedKey, quantity);
                    
                    // 防止预占库存为负数
                    if (newReserved != null && newReserved < 0) {
                        redisCacheService.set(reservedKey, 0, 7 * 24 * 60 * 60, TimeUnit.SECONDS);
                        log.warn("Redis预占库存为负数，已重置为0: productId={}", productId);
                    }

                    log.info("预占库存释放成功: productId={}, quantity={}, newReserved={}", 
                            productId, quantity, newReserved);
                    return true;

                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取库存锁超时: productId={}", productId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("预占库存释放被中断: productId={}, quantity={}", productId, quantity, e);
            return false;
        } catch (Exception e) {
            log.error("预占库存释放异常: productId={}, quantity={}", productId, quantity, e);
            return false;
        }
    }

    /**
     * 扣减总库存（双写模式：Redis + MySQL）
     * 支付订单时使用，同时扣减总库存
     * 使用分布式锁保证原子性
     */
    @Override
    @Transactional
    public boolean deductStock(Long productId, Integer quantity) {
        String lockKey = RedisConstants.getStockLockKey(productId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(RedisConstants.LOCK_WAIT_TIME, RedisConstants.LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    // 1. 更新MySQL总库存
                    int mysqlResult = productMapper.updateStock(productId, quantity);
                    if (mysqlResult <= 0) {
                        log.error("MySQL库存扣减失败: productId={}, quantity={}", productId, quantity);
                        return false;
                    }

                    // 2. 更新Redis总库存
                    String stockKey = RedisConstants.getStockKey(productId);
                    Long newStock = redisCacheService.decrement(stockKey, quantity);
                    
                    // 防止库存为负数
                    if (newStock != null && newStock < 0) {
                        redisCacheService.set(stockKey, 0, 7 * 24 * 60 * 60, TimeUnit.SECONDS);
                        log.warn("Redis库存为负数，已重置为0: productId={}", productId);
                    }

                    log.info("库存扣减成功: productId={}, quantity={}, newStock={}", 
                            productId, quantity, newStock);
                    return true;

                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取库存锁超时: productId={}", productId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("库存扣减被中断: productId={}, quantity={}", productId, quantity, e);
            return false;
        } catch (Exception e) {
            log.error("库存扣减异常: productId={}, quantity={}", productId, quantity, e);
            return false;
        }
    }

    /**
     * 获取可用库存
     * 优先从Redis获取，Redis不存在则从MySQL加载
     */
    @Override
    public Integer getAvailableStock(Long productId) {
        try {
            // 1. 尝试从Redis获取
            Integer availableStock = getAvailableStockFromRedis(productId);
            if (availableStock != null) {
                return availableStock;
            }

            // 2. Redis中没有，从MySQL加载并缓存到Redis
            return loadStockToRedis(productId);

        } catch (Exception e) {
            log.error("获取可用库存异常，降级到MySQL: productId={}", productId, e);
            // 降级：直接从MySQL获取
            Integer stock = productMapper.getAvailableStock(productId);
            return stock != null ? stock : 0;
        }
    }

    /**
     * 从Redis获取可用库存
     * 可用库存 = 总库存 - 预占库存
     */
    private Integer getAvailableStockFromRedis(Long productId) {
        String stockKey = RedisConstants.getStockKey(productId);
        String reservedKey = RedisConstants.getReservedStockKey(productId);

        Integer totalStock = redisCacheService.get(stockKey, Integer.class);
        Integer reservedStock = redisCacheService.get(reservedKey, Integer.class);

        if (totalStock == null) {
            return null; // Redis中没有数据
        }

        reservedStock = (reservedStock != null) ? reservedStock : 0;
        int available = totalStock - reservedStock;
        
        return Math.max(available, 0); // 确保不返回负数
    }

    /**
     * 从MySQL加载库存到Redis
     */
    private Integer loadStockToRedis(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            log.warn("商品不存在: productId={}", productId);
            return 0;
        }

        // 缓存到Redis（不设置过期时间，手动管理）
        String stockKey = RedisConstants.getStockKey(productId);
        String reservedKey = RedisConstants.getReservedStockKey(productId);

        redisCacheService.set(stockKey, product.getStock(), 7 * 24 * 60 * 60, TimeUnit.SECONDS);
        redisCacheService.set(reservedKey, product.getReservedStock() != null ? product.getReservedStock() : 0, 7 * 24 * 60 * 60, TimeUnit.SECONDS);

        int available = product.getStock() - (product.getReservedStock() != null ? product.getReservedStock() : 0);
        log.info("库存已加载到Redis: productId={}, stock={}, reserved={}, available={}", 
                productId, product.getStock(), product.getReservedStock(), available);

        return Math.max(available, 0);
    }

    @Override
    public Integer getUserCartQuantity(Long productId, Long userId) {
        Integer quantity = cartMapper.getUserCartQuantity(userId, productId);
        return quantity != null ? quantity : 0;
    }

    /**
     * 初始化商品库存到Redis
     * 用于系统启动或商品创建时
     */
    public void initStockToRedis(Long productId) {
        loadStockToRedis(productId);
    }

    /**
     * 清除商品库存缓存
     * 用于商品删除或需要强制刷新时
     */
    public void clearStockCache(Long productId) {
        String stockKey = RedisConstants.getStockKey(productId);
        String reservedKey = RedisConstants.getReservedStockKey(productId);
        
        redisCacheService.delete(stockKey);
        redisCacheService.delete(reservedKey);
        
        log.info("库存缓存已清除: productId={}", productId);
    }
}