package com.example.shop.controller;

import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.dto.LoginRequest;
import com.example.shop.dto.LoginResponse;
import com.example.shop.dto.RegisterRequest;
import com.example.shop.entity.User;
import com.example.shop.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @SystemLog(operation = "用户注册", module = "认证模块", action = "register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("role", "user");
        
        return Result.success("注册成功", data);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @SystemLog(operation = "用户登录", module = "认证模块", action = "login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @SystemLog(operation = "用户登出", module = "认证模块", action = "logout")
    public Result<Void> logout() {
        // JWT是无状态的，客户端删除token即可
        return Result.success("登出成功", null);
    }
}
