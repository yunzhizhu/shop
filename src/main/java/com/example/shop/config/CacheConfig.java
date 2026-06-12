package com.example.shop.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类
 * 用于优化消息系统的查询性能
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置缓存管理器
     * 使用内存缓存来提高查询性能
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // 配置缓存名称
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "conversationList",      // 会话列表缓存
            "totalUnreadCount",      // 总未读数缓存
            "privateUnreadCount",    // 私信未读数缓存
            "systemUnreadCount",     // 系统通知未读数缓存
            "conversationUnread",    // 会话未读数缓存
            "userInfo",              // 用户信息缓存
            "conversationInfo"       // 会话信息缓存
        ));
        
        return cacheManager;
    }
}