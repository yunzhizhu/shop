package com.example.shop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.dto.SystemStatisticsResponse;
import com.example.shop.entity.User;

import java.util.List;

/**
 * 管理员服务接口
 */
public interface AdminService {

    /**
     * 分页查询用户列表
     */
    IPage<User> getUserPage(int page, int size, String username, Integer roleId);

    /**
     * 根据ID获取用户信息
     */
    User getUserById(Long userId);

    /**
     * 更新用户状态
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 批量删除用户
     */
    void batchDeleteUsers(List<Long> userIds);

    /**
     * 重置用户密码
     */
    void resetUserPassword(Long userId, String newPassword);

    /**
     * 获取系统统计数据
     */
    SystemStatisticsResponse getSystemStatistics();
}
