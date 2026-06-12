package com.example.shop.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类
 * 用于分布式锁、延迟队列等高级功能
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private String port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    /**
     * 配置RedissonClient
     * 用于分布式锁、延迟队列等高级功能
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 单机模式配置
        String address = "redis://" + host + ":" + port;
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setConnectionPoolSize(20)
                .setConnectionMinimumIdleSize(5)
                .setIdleConnectionTimeout(10000)
                .setTimeout(3000);
        
        // 如果有密码，设置密码
        if (password != null && !password.isEmpty()) {
            config.useSingleServer().setPassword(password);
        }

        return Redisson.create(config);
    }
}
