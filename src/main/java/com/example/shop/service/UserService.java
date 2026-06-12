package com.example.shop.service;

import com.example.shop.dto.ChangePasswordRequest;
import com.example.shop.dto.UpdateProfileRequest;
import com.example.shop.dto.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 获取当前用户信息
     */
    UserProfileResponse getCurrentUserProfile();

    /**
     * 更新用户信息
     */
    void updateProfile(UpdateProfileRequest request);

    /**
     * 修改密码
     */
    void changePassword(ChangePasswordRequest request);

    /**
     * 上传头像
     */
    String uploadAvatar(MultipartFile file);

    /**
     * 更新用户余额
     */
    void updateBalance(Long userId, BigDecimal amount);

    /**
     * 更新用户积分
     */
    void updatePoints(Long userId, Integer points);
}
