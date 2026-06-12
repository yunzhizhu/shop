package com.example.shop.service.impl;

import com.example.shop.constants.RedisConstants;
import com.example.shop.entity.Cart;
import com.example.shop.mapper.CartMapper;
import com.example.shop.service.CartRedisService;
import com.example.shop.service.RedisCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 购物车Redis操作服务实现类
 * 使用Redis Hash结构存储购物车数据
 * 
 * Key: shopping:cart:user:{userId}
 * Field: {productId}
 * Value: Cart对象的JSON字符串
 * 
 * @author Kiro
 * @version 1.0
 * @since 2026-03-07
 */
@Slf4j
@Service
public class CartRedisServiceImpl implements CartRedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public List<Cart> getUserCart(Long userId) {
        String cacheKey = RedisConstants.getCartKey(userId);
        
        try {
            HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
            Map<String, Object> cartMap = hashOps.entries(cacheKey);

            
            if (cartMap == null || cartMap.isEmpty()) {
                // 检查是否是空购物车标记
                Object emptyFlag = hashOps.get(cacheKey, "empty");
                if ("true".equals(emptyFlag)) {
                    log.debug("用户购物车为空（空标记）: userId={}", userId);
                    return new ArrayList<>();
                }
                return null;
            }
            
            // 过滤掉空标记
            List<Cart> carts = cartMap.entrySet().stream()
                    .filter(entry -> !"empty".equals(entry.getKey()))
                    .map(entry -> {
                        try {
                            String json = objectMapper.writeValueAsString(entry.getValue());
                            return objectMapper.readValue(json, Cart.class);
                        } catch (Exception e) {
                            log.error("反序列化购物车项失败: userId={}, productId={}", 
                                    userId, entry.getKey(), e);
                            return null;
                        }
                    })
                    .filter(cart -> cart != null)
                    .collect(Collectors.toList());
            
            log.debug("从Redis获取购物车成功: userId={}, count={}", userId, carts.size());
            return carts;
            
        } catch (Exception e) {
            log.error("从Redis获取购物车失败: userId={}", userId, e);
            return null;
        }
    }

    @Override
    public Cart getCartItem(Long userId, Long productId) {
        String cacheKey = RedisConstants.getCartKey(userId);
        
        try {
            HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
            Object value = hashOps.get(cacheKey, productId.toString());
            
            if (value == null) {
                return null;
            }
            
            String json = objectMapper.writeValueAsString(value);
            Cart cart = objectMapper.readValue(json, Cart.class);
            
            log.debug("从Redis获取购物车项成功: userId={}, productId={}", userId, productId);
            return cart;
            
        } catch (Exception e) {
            log.error("从Redis获取购物车项失败: userId={}, productId={}", userId, productId, e);
            return null;
        }
    }

    @Override
    public void saveCartItem(Long userId, Cart cart) {
        String cacheKey = RedisConstants.getCartKey(userId);
        
        try {
            HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
            
            // 删除空标记（如果存在）
            hashOps.delete(cacheKey, "empty");
            
            // 保存购物车项
            hashOps.put(cacheKey, cart.getProductId().toString(), cart);
            
            // 设置过期时间（7天 + 随机0-1小时）
            long ttl = RedisConstants.getRandomTtl(RedisConstants.CART_TTL);
            redisTemplate.expire(cacheKey, ttl, TimeUnit.SECONDS);
            
            log.debug("保存购物车项到Redis成功: userId={}, productId={}, quantity={}", 
                    userId, cart.getProductId(), cart.getQuantity());
            
        } catch (Exception e) {
            log.error("保存购物车项到Redis失败: userId={}, productId={}", 
                    userId, cart.getProductId(), e);
            // 不抛出异常，允许Redis写入失败
        }
    }

    @Override
    public void saveCartItems(Long userId, List<Cart> carts) {
        String cacheKey = RedisConstants.getCartKey(userId);
        
        try {
            HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
            
            if (carts == null || carts.isEmpty()) {
                // 缓存空购物车标记（防止缓存穿透）
                hashOps.put(cacheKey, "empty", "true");
                redisTemplate.expire(cacheKey, RedisConstants.EMPTY_CACHE_TTL, TimeUnit.SECONDS);
                log.debug("缓存空购物车标记: userId={}", userId);
                return;
            }
            
            // 批量保存
            Map<String, Object> cartMap = carts.stream()
                    .collect(Collectors.toMap(
                            cart -> cart.getProductId().toString(),
                            cart -> cart
                    ));
            
            hashOps.putAll(cacheKey, cartMap);
            
            // 设置过期时间（7天 + 随机0-1小时）
            long ttl = RedisConstants.getRandomTtl(RedisConstants.CART_TTL);
            redisTemplate.expire(cacheKey, ttl, TimeUnit.SECONDS);
            
            log.debug("批量保存购物车到Redis成功: userId={}, count={}", userId, carts.size());
            
        } catch (Exception e) {
            log.error("批量保存购物车到Redis失败: userId={}", userId, e);
            // 不抛出异常，允许Redis写入失败
        }
    }

    @Override
    public void deleteCartItem(Long userId, Long productId) {
        String cacheKey = RedisConstants.getCartKey(userId);
        
        try {
            HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
            hashOps.delete(cacheKey, productId.toString());
            
            log.debug("从Redis删除购物车项成功: userId={}, productId={}", userId, productId);
            
        } catch (Exception e) {
            log.error("从Redis删除购物车项失败: userId={}, productId={}", userId, productId, e);
            // 不抛出异常，允许Redis操作失败
        }
    }

    @Override
    public void deleteCartItems(Long userId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        
        String cacheKey = RedisConstants.getCartKey(userId);
        
        try {
            // 使用Pipeline批量删除，减少网络往返
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
                for (Long productId : productIds) {
                    hashOps.delete(cacheKey, productId.toString());
                }
                return null;
            });
            
            log.debug("从Redis批量删除购物车项成功（Pipeline）: userId={}, count={}", 
                    userId, productIds.size());
            
        } catch (Exception e) {
            log.error("从Redis批量删除购物车项失败: userId={}", userId, e);
            // 不抛出异常，允许Redis操作失败
        }
    }

    @Override
    public void clearCart(Long userId) {
        String cacheKey = RedisConstants.getCartKey(userId);
        
        try {
            redisTemplate.delete(cacheKey);
            log.debug("清空Redis购物车成功: userId={}", userId);
            
        } catch (Exception e) {
            log.error("清空Redis购物车失败: userId={}", userId, e);
            // 不抛出异常，允许Redis操作失败
        }
    }

    @Override
    public boolean existsCart(Long userId) {
        String cacheKey = RedisConstants.getCartKey(userId);
        
        try {
            Boolean exists = redisTemplate.hasKey(cacheKey);
            return exists != null && exists;
            
        } catch (Exception e) {
            log.error("检查Redis购物车存在性失败: userId={}", userId, e);
            return false;
        }
    }

    @Override
    public void refreshExpire(Long userId) {
        String cacheKey = RedisConstants.getCartKey(userId);
        
        try {
            // 刷新过期时间（7天 + 随机0-1小时）
            long ttl = RedisConstants.getRandomTtl(RedisConstants.CART_TTL);
            redisTemplate.expire(cacheKey, ttl, TimeUnit.SECONDS);
            
            log.debug("刷新Redis购物车过期时间成功: userId={}, ttl={}秒", userId, ttl);
            
        } catch (Exception e) {
            log.error("刷新Redis购物车过期时间失败: userId={}", userId, e);
            // 不抛出异常，允许Redis操作失败
        }
    }

    @Override
    public void loadFromDatabase(Long userId) {
        String lockKey = RedisConstants.getCacheLoadLockKey("cart", userId);
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试获取锁，等待3秒，持有10秒
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    // 双重检查：获取锁后再次检查缓存
                    if (existsCart(userId)) {
                        log.debug("购物车已被其他线程加载: userId={}", userId);
                        return;
                    }
                    
                    // 从MySQL查询购物车数据
                    List<Cart> carts = cartMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Cart>()
                            .eq(Cart::getUserId, userId)
                    );
                    
                    // 保存到Redis
                    saveCartItems(userId, carts);
                    
                    log.info("从MySQL加载购物车到Redis成功（分布式锁）: userId={}, count={}", 
                            userId, carts.size());
                    
                } finally {
                    lock.unlock();
                }
            } else {
                // 获取锁失败，等待一下再检查缓存
                Thread.sleep(50);
                if (existsCart(userId)) {
                    log.debug("等待后购物车已加载: userId={}", userId);
                } else {
                    log.warn("获取锁超时，购物车加载失败: userId={}", userId);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("加载购物车被中断: userId={}", userId, e);
        } catch (Exception e) {
            log.error("从MySQL加载购物车到Redis失败: userId={}", userId, e);
        }
    }

    @Override
    public Integer incrementQuantity(Long userId, Long productId, Integer quantity, Cart cart) {
        String cacheKey = RedisConstants.getCartKey(userId);
        String field = productId.toString();
        
        try {
            // Lua脚本：原子性增加数量
            String luaScript = 
                "local current = redis.call('HGET', KEYS[1], ARGV[1]) " + // 从Hash中取出该商品的购物车项JSON
                "if current then " + // 商品已在购物车中，执行数量累加
                "  local cartItem = cjson.decode(current) " + // 反序列化JSON为Lua table
                "  cartItem.quantity = cartItem.quantity + tonumber(ARGV[2]) " + // 累加购买数量
                "  cartItem.updatedAt = ARGV[3] " + // 更新修改时间
                "  cartItem.isSelected = 1 " + // 新增/更新后默认选中
                "  redis.call('HSET', KEYS[1], ARGV[1], cjson.encode(cartItem)) " + // 序列化后写回Hash
                "  return cartItem.quantity " + // 返回更新后的数量
                "else " + // 商品不在购物车中，直接新增
                "  redis.call('HSET', KEYS[1], ARGV[1], ARGV[4]) " + // 将新购物车项JSON写入Hash
                "  return tonumber(ARGV[2]) " + // 返回初始数量
                "end";
            
            // 准备参数
            String cartJson = objectMapper.writeValueAsString(cart);
            String updatedAt = java.time.LocalDateTime.now().toString();
            
            // 执行Lua脚本
            Long result = redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
                java.util.Collections.singletonList(cacheKey),
                field, quantity.toString(), updatedAt, cartJson
            );
            
            // 刷新过期时间
            refreshExpire(userId);
            
            log.debug("原子性增加购物车数量成功（Lua）: userId={}, productId={}, quantity={}, result={}", 
                    userId, productId, quantity, result);
            
            return result != null ? result.intValue() : quantity;
            
        } catch (Exception e) {
            log.error("原子性增加购物车数量失败: userId={}, productId={}", userId, productId, e);
            // 降级到普通保存
            saveCartItem(userId, cart);
            return cart.getQuantity();
        }
    }
}
