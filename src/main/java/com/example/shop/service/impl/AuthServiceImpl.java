package com.example.shop.service.impl;

import com.example.shop.dto.LoginRequest;
import com.example.shop.dto.LoginResponse;
import com.example.shop.dto.RegisterRequest;
import com.example.shop.entity.User;
import com.example.shop.enums.UserRole;
import com.example.shop.enums.UserStatus;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.UserMapper;
import com.example.shop.service.AuthService;
import com.example.shop.service.CartSyncService;
import com.example.shop.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 认证服务实现类
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CartSyncService cartSyncService;

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException(1001, "用户名已存在");
        }

        // 生成盐值
        String salt = generateSalt();
        
        // 加密密码
        String passwordHash = passwordEncoder.encode(request.getPassword() + salt);

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordHash);
        user.setSalt(salt);
        user.setRoleId(UserRole.USER.getRoleId());
        user.setBalance(BigDecimal.ZERO);
        user.setPoints(0);
        user.setStatus(UserStatus.NORMAL.getCode());

        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new BusinessException("注册失败");
        }

        log.info("用户注册成功: {}", request.getUsername());
        return user;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            // 查询用户
            User user = userMapper.selectByUsername(request.getUsername());
            if (user == null) {
                throw new BusinessException(401, "用户名或密码错误");
            }

            // 检查用户状态
            if (UserStatus.DISABLED.getCode().equals(user.getStatus())) {
                throw new BusinessException(403, "账户已被禁用");
            }

            // 验证密码
            if (!passwordEncoder.matches(request.getPassword() + user.getSalt(), user.getPasswordHash())) {
                throw new BusinessException(401, "用户名或密码错误");
            }

            // 更新最后登录时间
            userMapper.updateLastLoginTime(user.getUserId());

            // 同步用户购物车数据（确保数据一致性）
            try {
                cartSyncService.syncUserCartData(user.getUserId());
                log.info("用户登录时购物车数据同步完成: userId={}", user.getUserId());
            } catch (Exception e) {
                log.warn("用户登录时购物车数据同步失败: userId={}, error={}", user.getUserId(), e.getMessage());
                // 不影响登录流程，只记录警告日志
            }

            // 生成JWT Token
            UserRole userRole = UserRole.getByRoleId(user.getRoleId());
            String role = userRole != null ? userRole.getRoleName() : "user";
            String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), role);

            // 构建响应
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setExpiresIn(7200L); // 2小时

            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setUserId(user.getUserId());
            userInfo.setUsername(user.getUsername());
            userInfo.setRole(role);
            userInfo.setAvatar(user.getAvatar());
            userInfo.setPhone(user.getPhone());
            response.setUserInfo(userInfo);

            log.info("用户登录成功: {}", request.getUsername());
            return response;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage(), e);
            throw new BusinessException("登录失败");
        }
    }

    /**
     * 生成盐值
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}
