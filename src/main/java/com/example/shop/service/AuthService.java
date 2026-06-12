package com.example.shop.service;

import com.example.shop.dto.LoginRequest;
import com.example.shop.dto.LoginResponse;
import com.example.shop.dto.RegisterRequest;
import com.example.shop.entity.User;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     */
    User register(RegisterRequest request);

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);
}
