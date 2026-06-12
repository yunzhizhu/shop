package com.example.shop.service;

import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务接口
 * 封装常用的缓存操作
 */
public interface RedisCacheService {

    /**
     * 设置缓存
     */
    void set(String key, Object value);

    /**
     * 设置缓存（带过期时间）
     */
    void set(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 获取缓存
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 删除缓存
     */
    void delete(String key);

    /**
     * 判断缓存是否存在
     */
    boolean hasKey(String key);

    /**
     * 设置过期时间
     */
    boolean expire(String key, long timeout, TimeUnit unit);

    /**
     * 递增
     */
    Long increment(String key);

    /**
     * 递增指定值
     */
    Long increment(String key, long delta);

    /**
     * 递减
     */
    Long decrement(String key);

    /**
     * 递减指定值
     */
    Long decrement(String key, long delta);
}
