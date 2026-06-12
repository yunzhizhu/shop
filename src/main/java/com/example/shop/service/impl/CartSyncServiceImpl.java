package com.example.shop.service.impl;

import com.example.shop.entity.Cart;
import com.example.shop.entity.Product;
import com.example.shop.mapper.CartMapper;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.service.CartSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 购物车数据同步服务实现类
 */
@Slf4j
@Service
public class CartSyncServiceImpl implements CartSyncService {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Override
    @Transactional
    public void syncUserCartData(Long userId) {
        try {
            log.info("开始同步用户购物车数据: userId={}", userId);
            
            // v3.0 订单预占模式：购物车不预占库存，只需清理无效商品
            // 清理无效的购物车项（商品不存在或已下架）
            cleanInvalidCartItems(userId);
            
            log.info("用户购物车数据同步完成: userId={}", userId);
                    
        } catch (Exception e) {
            log.error("同步用户购物车数据失败: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 清理无效的购物车项
     */
    private void cleanInvalidCartItems(Long userId) {
        try {
            // 查询用户购物车中商品不存在或已下架的项
            List<Cart> invalidCarts = cartMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Cart>()
                    .eq("user_id", userId)
                    .notExists("SELECT 1 FROM product p WHERE p.product_id = cart.product_id AND p.status = 1")
            );
            
            if (!invalidCarts.isEmpty()) {
                List<Long> invalidCartIds = invalidCarts.stream()
                    .map(Cart::getCartId)
                    .collect(java.util.stream.Collectors.toList());
                
                // 删除无效的购物车项
                cartMapper.deleteByUserIdAndCartIds(userId, invalidCartIds);
                
                log.info("清理无效购物车项: userId={}, 清理数量={}", userId, invalidCarts.size());
            }
        } catch (Exception e) {
            log.warn("清理无效购物车项失败: userId={}", userId, e);
        }
    }

    @Override
    @Transactional
    public void forceFixCartDataInconsistency(Long userId) {
        try {
            log.info("开始强制修复用户购物车数据不一致问题: userId={}", userId);
            
            // v3.0 订单预占模式：购物车不预占库存
            // 只需清理无效商品和检查数量限制
            
            // 1. 获取用户所有购物车项
            List<Cart> userCarts = cartMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Cart>()
                    .eq("user_id", userId)
            );
            
            if (userCarts.isEmpty()) {
                log.info("用户购物车为空，无需修复: userId={}", userId);
                return;
            }
            
            // 2. 按商品分组，检查每个商品的数据一致性
            Map<Long, List<Cart>> cartsByProduct = userCarts.stream()
                .collect(java.util.stream.Collectors.groupingBy(Cart::getProductId));
            
            for (Map.Entry<Long, List<Cart>> entry : cartsByProduct.entrySet()) {
                Long productId = entry.getKey();
                List<Cart> productCarts = entry.getValue();
                
                // 检查商品是否存在且上架
                Product product = productMapper.selectById(productId);
                if (product == null || product.getStatus() != 1) {
                    // 删除无效商品的购物车项
                    List<Long> invalidCartIds = productCarts.stream()
                        .map(Cart::getCartId)
                        .collect(java.util.stream.Collectors.toList());
                    cartMapper.deleteByUserIdAndCartIds(userId, invalidCartIds);
                    log.info("删除无效商品的购物车项: productId={}, 删除数量={}", productId, invalidCartIds.size());
                    continue;
                }
                
                // v3.0 订单预占模式：购物车不限制数量
                // 只需确保购物车数据有效即可，库存校验在提交订单时进行
            }
            
            log.info("强制修复用户购物车数据完成: userId={}", userId);
            
        } catch (Exception e) {
            log.error("强制修复用户购物车数据失败: userId={}", userId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void syncAllCartData() {
        try {
            log.info("开始同步所有购物车数据");
            
            // v3.0 订单预占模式：购物车不预占库存
            // 此方法已废弃，保留空实现以兼容现有接口
            log.warn("syncAllCartData 方法已废弃（v3.0 订单预占模式），购物车不再预占库存");
            
            log.info("所有购物车数据同步完成（无操作）");
            
        } catch (Exception e) {
            log.error("同步所有购物车数据失败", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void syncProductReservedStock(Long productId) {
        try {
            // v3.0 订单预占模式：购物车不预占库存
            // 预占库存只由订单管理，此方法已废弃
            log.warn("syncProductReservedStock 方法已废弃（v3.0 订单预占模式），购物车不再预占库存: productId={}", productId);
            
            // 保留空实现以兼容现有接口
            
        } catch (Exception e) {
            log.error("同步商品预占库存失败: productId={}", productId, e);
            throw e;
        }
    }
}