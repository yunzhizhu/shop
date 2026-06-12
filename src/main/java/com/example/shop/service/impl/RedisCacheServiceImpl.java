package com.example.shop.service.impl;

import com.example.shop.service.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务实现类
 * 使用Spring Data Redis进行基础缓存操作
 */
@Slf4j
@Service
public class RedisCacheServiceImpl implements RedisCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Redis set: key={}", key);
        } catch (Exception e) {
            log.error("Redis set error: key={}", key, e);
        }
    }

    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Redis set with expire: key={}, timeout={} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis set with expire error: key={}", key, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("Redis get: key={}, value=null", key);
                return null;
            }
            log.debug("Redis get: key={}, found", key);
            return (T) value;
        } catch (Exception e) {
            log.error("Redis get error: key={}", key, e);
            return null;
        }
    }

    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Redis delete: key={}", key);
        } catch (Exception e) {
            log.error("Redis delete error: key={}", key, e);
        }
    }

    @Override
    public boolean hasKey(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return result != null && result;
        } catch (Exception e) {
            log.error("Redis hasKey error: key={}", key, e);
            return false;
        }
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            Boolean result = redisTemplate.expire(key, timeout, unit);
            log.debug("Redis expire: key={}, timeout={} {}", key, timeout, unit);
            return result != null && result;
        } catch (Exception e) {
            log.error("Redis expire error: key={}", key, e);
            return false;
        }
    }

    @Override
    public Long increment(String key) {
        try {
            Long result = redisTemplate.opsForValue().increment(key);
            log.debug("Redis increment: key={}, result={}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Redis increment error: key={}", key, e);
            return null;
        }
    }

    @Override
    public Long increment(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().increment(key, delta);
            log.debug("Redis increment: key={}, delta={}, result={}", key, delta, result);
            return result;
        } catch (Exception e) {
            log.error("Redis increment error: key={}, delta={}", key, delta, e);
            return null;
        }
    }

    @Override
    public Long decrement(String key) {
        try {
            Long result = redisTemplate.opsForValue().decrement(key);
            log.debug("Redis decrement: key={}, result={}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Redis decrement error: key={}", key, e);
            return null;
        }
    }

    @Override
    public Long decrement(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().decrement(key, delta);
            log.debug("Redis decrement: key={}, delta={}, result={}", key, delta, result);
            return result;
        } catch (Exception e) {
            log.error("Redis decrement error: key={}, delta={}", key, delta, e);
            return null;
        }
    }
}
