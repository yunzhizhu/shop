package com.example.shop.utils;

import com.example.shop.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 * 用于生成和解析JWT令牌，实现无状态认证
 * 
 * JWT结构：Header.Payload.Signature
 * - Header: 包含算法和令牌类型
 * - Payload: 包含用户信息（声明）
 * - Signature: 签名，防止令牌被篡改
 */
@Slf4j
@Component
public class JwtUtil {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 生成JWT Token
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param role 用户角色（admin/user）
     * @return JWT令牌字符串
     * 
     * 示例：
     * generateToken(2001L, "admin", "admin")
     * 返回：eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjIwMDEsInVzZXJuYW1lIjoiYWRtaW4ifQ.signature
     */
    public String generateToken(Long userId, String username, String role) {
        // 创建声明（Claims）- 存储在Token的Payload中
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);      // 用户ID，用于后续请求识别用户
        claims.put("username", username);  // 用户名
        claims.put("role", role);          // 角色，用于权限控制
        
        return createToken(claims, username);
    }

    /**
     * 创建Token
     * 
     * @param claims 自定义声明（用户信息）
     * @param subject 主题（通常是用户名）
     * @return JWT令牌字符串
     * 
     * 工作流程：
     * 1. 设置当前时间和过期时间
     * 2. 使用密钥生成签名
     * 3. 构建JWT令牌
     */
    private String createToken(Map<String, Object> claims, String subject) {
        // 1. 获取当前时间
        Date now = new Date();
        
        // 2. 计算过期时间
        // jwtProperties.getExpiration() 返回秒数（如7200秒=2小时）
        // 乘以1000转换为毫秒
        Date expiration = new Date(now.getTime() + jwtProperties.getExpiration() * 1000);
        
        // 3. 生成密钥
        // 使用HMAC-SHA256算法，基于配置的密钥字符串生成SecretKey对象
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
        
        // 4. 构建JWT令牌
        return Jwts.builder()
                .setClaims(claims)           // 设置自定义声明（Payload部分）
                .setSubject(subject)         // 设置主题（标准声明）
                .setIssuedAt(now)           // 设置签发时间（iat）
                .setExpiration(expiration)   // 设置过期时间（exp）
                .signWith(key, SignatureAlgorithm.HS256)  // 使用HS256算法签名
                .compact();                  // 生成最终的JWT字符串（Header.Payload.Signature）
    }

    /**
     * 从Token中获取用户名
     * 
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从Token中获取用户ID
     * 
     * @param token JWT令牌
     * @return 用户ID
     * 
     * 使用场景：
     * 在Controller中获取当前登录用户的ID
     * Long userId = jwtUtil.getUserIdFromToken(token);
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取角色
     * 
     * @param token JWT令牌
     * @return 用户角色
     * 
     * 使用场景：
     * 在权限控制中判断用户角色
     * String role = jwtUtil.getRoleFromToken(token);
     * if ("admin".equals(role)) { ... }
     */
    public String getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 从Token中获取过期时间
     * 
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从Token中获取指定声明
     * 
     * @param token JWT令牌
     * @param claimsResolver 声明解析函数
     * @return 声明值
     * 
     * 这是一个通用方法，使用函数式编程
     * 例如：getClaimFromToken(token, Claims::getSubject)
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从Token中获取所有声明
     * 
     * @param token JWT令牌
     * @return 所有声明（Claims对象）
     * 
     * 工作流程：
     * 1. 使用密钥创建解析器
     * 2. 解析JWT令牌
     * 3. 验证签名
     * 4. 返回Payload中的所有声明
     * 
     * 如果签名验证失败，会抛出异常
     */
    private Claims getAllClaimsFromToken(String token) {
        // 使用相同的密钥生成SecretKey对象
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
        
        // 解析JWT令牌
        return Jwts.parserBuilder()
                .setSigningKey(key)          // 设置签名密钥（用于验证签名）
                .build()                     // 构建解析器
                .parseClaimsJws(token)       // 解析JWT令牌（会验证签名）
                .getBody();                  // 获取Payload部分（Claims）
    }

    /**
     * 检查Token是否过期
     * 
     * @param token JWT令牌
     * @return true-已过期，false-未过期
     * 
     * 使用场景：
     * 在认证过滤器中检查令牌是否有效
     * if (jwtUtil.isTokenExpired(token)) {
     *     throw new BusinessException("令牌已过期");
     * }
     */
    public Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            // 比较过期时间和当前时间
            return expiration.before(new Date());
        } catch (Exception e) {
            // 如果解析失败（如签名错误），也认为是过期
            return true;
        }
    }

    /**
     * 验证Token
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String tokenUsername = getUsernameFromToken(token);
            return (username.equals(tokenUsername) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析Token（不验证签名，用于获取过期Token中的信息）
     */
    public Claims parseTokenWithoutValidation(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (Exception e) {
            log.error("Token解析失败: {}", e.getMessage());
            return null;
        }
    }
}
