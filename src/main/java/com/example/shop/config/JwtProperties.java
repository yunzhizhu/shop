package com.example.shop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT密钥
     */
    private String secret;

    /**
     * JWT过期时间（秒）
     */
    private Long expiration;

    /**
     * JWT请求头
     */
    private String header;

    /**
     * JWT前缀
     */
    private String prefix;
}
