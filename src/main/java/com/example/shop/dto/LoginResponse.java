package com.example.shop.dto;

import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse {

    /**
     * JWT令牌
     */
    private String token;

    /**
     * Token过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户信息
     */
    private UserInfo userInfo;

    @Data
    public static class UserInfo {
        private Long userId;
        private String username;
        private String role;
        private String avatar;
        private String phone;
    }
}
