package com.example.shop.service;

import com.example.shop.entity.Cart;
import com.example.shop.mapper.CartMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 购物车Redis集成测试
 * 测试Redis缓存功能和数据一致性
 * 
 * @author Kiro
 * @version 1.0
 * @since 2026-03-07
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class CartRedisIntegrationTest {

    @Autowired
    private CartRedisService cartRedisService;

    @Autowired
    private CartMapper cartMapper;

    private Long testUserId = 999L;
    private Long testProductId1 = 1L;
    private Long testProductId2 = 2L;

    @BeforeEach
    public void setUp() {
        // 清理测试数据
        cartRedisService.clearCart(testUserId);
        log.info("测试环境准备完成");
    }

    @Test
    public void testSaveAndGetCartItem() {
        log.info("测试：保存和获取购物车项");
        
        // 创建测试数据
        Cart cart = createTestCart(testUserId, testProductId1, 2);
        
        // 保存到Redis
        cartRedisService.saveCartItem(testUserId, cart);
        
        // 从Redis获取
        Cart retrieved = cartRedisService.getCartItem(testUserId, testProductId1);
        
        // 验证
        assertNotNull(retrieved, "购物车项应该存在");
        assertEquals(testProductId1, retrieved.getProductId(), "商品ID应该匹配");
        assertEquals(2, retrieved.getQuantity(), "数量应该匹配");
        
        log.info("测试通过：保存和获取购物车项");
    }

    @Test
    public void testGetUserCart() {
        log.info("测试：获取用户购物车");
        
        // 创建多个测试数据
        Cart cart1 = createTestCart(testUserId, testProductId1, 2);
        Cart cart2 = createTestCart(testUserId, testProductId2, 3);
        
        // 保存到Redis
        cartRedisService.saveCartItem(testUserId, cart1);
        cartRedisService.saveCartItem(testUserId, cart2);
        
        // 获取整个购物车
        List<Cart> carts = cartRedisService.getUserCart(testUserId);
        
        // 验证
        assertNotNull(carts, "购物车应该存在");
        assertEquals(2, carts.size(), "购物车应该有2个商品");
        
        log.info("测试通过：获取用户购物车");
    }

    @Test
    public void testDeleteCartItem() {
        log.info("测试：删除购物车项");
        
        // 创建测试数据
        Cart cart = createTestCart(testUserId, testProductId1, 2);
        cartRedisService.saveCartItem(testUserId, cart);
        
        // 验证存在
        assertTrue(cartRedisService.getCartItem(testUserId, testProductId1) != null);
        
        // 删除
        cartRedisService.deleteCartItem(testUserId, testProductId1);
        
        // 验证已删除
        assertNull(cartRedisService.getCartItem(testUserId, testProductId1), 
                "购物车项应该已被删除");
        
        log.info("测试通过：删除购物车项");
    }

    @Test
    public void testBatchDeleteCartItems() {
        log.info("测试：批量删除购物车项");
        
        // 创建多个测试数据
        Cart cart1 = createTestCart(testUserId, testProductId1, 2);
        Cart cart2 = createTestCart(testUserId, testProductId2, 3);
        cartRedisService.saveCartItem(testUserId, cart1);
        cartRedisService.saveCartItem(testUserId, cart2);
        
        // 批量删除
        cartRedisService.deleteCartItems(testUserId, Arrays.asList(testProductId1, testProductId2));
        
        // 验证已删除
        List<Cart> carts = cartRedisService.getUserCart(testUserId);
        assertTrue(carts == null || carts.isEmpty(), "购物车应该为空");
        
        log.info("测试通过：批量删除购物车项");
    }

    @Test
    public void testClearCart() {
        log.info("测试：清空购物车");
        
        // 创建测试数据
        Cart cart1 = createTestCart(testUserId, testProductId1, 2);
        Cart cart2 = createTestCart(testUserId, testProductId2, 3);
        cartRedisService.saveCartItem(testUserId, cart1);
        cartRedisService.saveCartItem(testUserId, cart2);
        
        // 清空购物车
        cartRedisService.clearCart(testUserId);
        
        // 验证
        assertFalse(cartRedisService.existsCart(testUserId), "购物车应该不存在");
        
        log.info("测试通过：清空购物车");
    }

    @Test
    public void testExistsCart() {
        log.info("测试：检查购物车是否存在");
        
        // 初始状态：不存在
        assertFalse(cartRedisService.existsCart(testUserId), "购物车初始应该不存在");
        
        // 添加商品
        Cart cart = createTestCart(testUserId, testProductId1, 2);
        cartRedisService.saveCartItem(testUserId, cart);
        
        // 验证存在
        assertTrue(cartRedisService.existsCart(testUserId), "购物车应该存在");
        
        log.info("测试通过：检查购物车是否存在");
    }

    @Test
    public void testEmptyCartCache() {
        log.info("测试：空购物车缓存（防穿透）");
        
        // 保存空购物车
        cartRedisService.saveCartItems(testUserId, Arrays.asList());
        
        // 验证缓存存在
        assertTrue(cartRedisService.existsCart(testUserId), "空购物车缓存应该存在");
        
        // 获取购物车
        List<Cart> carts = cartRedisService.getUserCart(testUserId);
        
        // 验证返回空列表
        assertNotNull(carts, "应该返回空列表而不是null");
        assertTrue(carts.isEmpty(), "购物车应该为空");
        
        log.info("测试通过：空购物车缓存");
    }

    @Test
    public void testRefreshExpire() {
        log.info("测试：刷新过期时间");
        
        // 创建测试数据
        Cart cart = createTestCart(testUserId, testProductId1, 2);
        cartRedisService.saveCartItem(testUserId, cart);
        
        // 刷新过期时间
        cartRedisService.refreshExpire(testUserId);
        
        // 验证仍然存在
        assertTrue(cartRedisService.existsCart(testUserId), "购物车应该仍然存在");
        
        log.info("测试通过：刷新过期时间");
    }

    @Test
    public void testIncrementQuantity() {
        log.info("测试：原子性增加数量（Lua脚本）");
        
        // 创建测试数据
        Cart cart = createTestCart(testUserId, testProductId1, 2);
        
        // 第一次增加（新增）
        Integer quantity1 = cartRedisService.incrementQuantity(testUserId, testProductId1, 2, cart);
        assertEquals(2, quantity1, "第一次增加应该返回2");
        
        // 第二次增加（累加）
        cart.setQuantity(3);
        Integer quantity2 = cartRedisService.incrementQuantity(testUserId, testProductId1, 3, cart);
        assertEquals(5, quantity2, "第二次增加应该返回5（2+3）");
        
        // 验证最终数量
        Cart retrieved = cartRedisService.getCartItem(testUserId, testProductId1);
        assertEquals(5, retrieved.getQuantity(), "最终数量应该是5");
        
        log.info("测试通过：原子性增加数量");
    }

    /**
     * 创建测试购物车数据
     */
    private Cart createTestCart(Long userId, Long productId, Integer quantity) {
        Cart cart = new Cart();
        cart.setCartId(System.currentTimeMillis()); // 使用时间戳作为临时ID
        cart.setUserId(userId);
        cart.setProductId(productId);
        cart.setQuantity(quantity);
        cart.setIsSelected(1);
        cart.setVersion(0);
        cart.setCreatedAt(java.time.LocalDateTime.now());
        cart.setUpdatedAt(java.time.LocalDateTime.now());
        return cart;
    }
}
