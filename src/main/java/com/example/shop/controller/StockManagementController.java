package com.example.shop.controller;

import com.example.shop.common.Result;
import com.example.shop.constants.RedisConstants;
import com.example.shop.entity.Product;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.service.RedisCacheService;
import com.example.shop.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存管理控制器
 * 用于库存初始化、同步和测试
 */
@Slf4j
@RestController
@RequestMapping("/admin/stock")
public class StockManagementController {

    @Autowired
    private StockService stockService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    /**
     * 初始化单个商品库存到Redis
     */
    @PostMapping("/init/{productId}")
    public Result<Map<String, Object>> initStock(@PathVariable Long productId) {
        try {
            stockService.initStockToRedis(productId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("productId", productId);
            result.put("availableStock", stockService.getAvailableStock(productId));
            result.put("status", "初始化成功");
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("初始化库存失败: productId={}", productId, e);
            return Result.error("初始化失败: " + e.getMessage());
        }
    }

    /**
     * 初始化所有商品库存到Redis
     */
    @PostMapping("/init/all")
    public Result<Map<String, Object>> initAllStock() {
        try {
            List<Product> products = productMapper.selectList(null);
            int successCount = 0;
            int failCount = 0;

            for (Product product : products) {
                try {
                    stockService.initStockToRedis(product.getProductId());
                    successCount++;
                } catch (Exception e) {
                    log.error("初始化库存失败: productId={}", product.getProductId(), e);
                    failCount++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", products.size());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("status", "批量初始化完成");

            return Result.success(result);
        } catch (Exception e) {
            log.error("批量初始化库存失败", e);
            return Result.error("批量初始化失败: " + e.getMessage());
        }
    }

    /**
     * 查看商品库存详情（Redis + MySQL）
     */
    @GetMapping("/detail/{productId}")
    public Result<Map<String, Object>> getStockDetail(@PathVariable Long productId) {
        try {
            // MySQL数据
            Product product = productMapper.selectById(productId);
            if (product == null) {
                return Result.error("商品不存在");
            }

            // Redis数据
            String stockKey = RedisConstants.getStockKey(productId);
            String reservedKey = RedisConstants.getReservedStockKey(productId);
            Integer redisStock = redisCacheService.get(stockKey, Integer.class);
            Integer redisReserved = redisCacheService.get(reservedKey, Integer.class);

            Map<String, Object> result = new HashMap<>();
            
            // MySQL数据
            Map<String, Object> mysqlData = new HashMap<>();
            mysqlData.put("stock", product.getStock());
            mysqlData.put("reservedStock", product.getReservedStock());
            mysqlData.put("availableStock", product.getStock() - (product.getReservedStock() != null ? product.getReservedStock() : 0));
            result.put("mysql", mysqlData);

            // Redis数据
            Map<String, Object> redisData = new HashMap<>();
            redisData.put("stock", redisStock);
            redisData.put("reservedStock", redisReserved);
            redisData.put("availableStock", redisStock != null ? redisStock - (redisReserved != null ? redisReserved : 0) : null);
            redisData.put("cached", redisStock != null);
            result.put("redis", redisData);

            // 一致性检查
            boolean consistent = true;
            if (redisStock != null) {
                consistent = redisStock.equals(product.getStock()) && 
                           (redisReserved != null ? redisReserved : 0) == (product.getReservedStock() != null ? product.getReservedStock() : 0);
            }
            result.put("consistent", consistent);

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取库存详情失败: productId={}", productId, e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 清除商品库存缓存
     */
    @DeleteMapping("/cache/{productId}")
    public Result<String> clearCache(@PathVariable Long productId) {
        try {
            stockService.clearStockCache(productId);
            return Result.success("缓存已清除");
        } catch (Exception e) {
            log.error("清除缓存失败: productId={}", productId, e);
            return Result.error("清除失败: " + e.getMessage());
        }
    }

    /**
     * 测试库存预占
     */
    @PostMapping("/test/reserve")
    public Result<Map<String, Object>> testReserve(@RequestParam Long productId, @RequestParam Integer quantity) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 预占前的库存
            Integer beforeAvailable = stockService.getAvailableStock(productId);
            result.put("beforeAvailable", beforeAvailable);

            // 执行预占
            boolean success = stockService.reserveStock(productId, quantity);
            result.put("reserveSuccess", success);

            // 预占后的库存
            Integer afterAvailable = stockService.getAvailableStock(productId);
            result.put("afterAvailable", afterAvailable);
            result.put("changed", beforeAvailable - afterAvailable);

            return Result.success(result);
        } catch (Exception e) {
            log.error("测试库存预占失败", e);
            return Result.error("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试释放预占库存
     */
    @PostMapping("/test/release")
    public Result<Map<String, Object>> testRelease(@RequestParam Long productId, @RequestParam Integer quantity) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 释放前的库存
            Integer beforeAvailable = stockService.getAvailableStock(productId);
            result.put("beforeAvailable", beforeAvailable);

            // 执行释放
            boolean success = stockService.releaseReservedStock(productId, quantity);
            result.put("releaseSuccess", success);

            // 释放后的库存
            Integer afterAvailable = stockService.getAvailableStock(productId);
            result.put("afterAvailable", afterAvailable);
            result.put("changed", afterAvailable - beforeAvailable);

            return Result.success(result);
        } catch (Exception e) {
            log.error("测试释放预占库存失败", e);
            return Result.error("测试失败: " + e.getMessage());
        }
    }
}
