package com.example.shop.service.impl;

import com.example.shop.entity.Product;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 库存初始化器
 * 应用启动时自动同步MySQL库存到Redis
 */
@Slf4j
@Component
public class StockInitializer implements CommandLineRunner {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StockService stockService;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== 开始初始化库存到Redis ===");
        
        try {
            // 获取所有商品
            List<Product> products = productMapper.selectList(null);
            
            int successCount = 0;
            int failCount = 0;
            
            for (Product product : products) {
                try {
                    stockService.initStockToRedis(product.getProductId());
                    successCount++;
                    log.debug("库存同步成功: productId={}, stock={}, reserved={}", 
                             product.getProductId(), product.getStock(), product.getReservedStock());
                } catch (Exception e) {
                    failCount++;
                    log.error("库存同步失败: productId={}, error={}", product.getProductId(), e.getMessage());
                }
            }
            
            log.info("=== 库存初始化完成：成功={}, 失败={}, 总计={} ===", 
                     successCount, failCount, products.size());
                     
        } catch (Exception e) {
            log.error("库存初始化失败", e);
        }
    }
}
