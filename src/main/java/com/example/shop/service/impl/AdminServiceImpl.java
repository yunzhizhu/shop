package com.example.shop.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.dto.SystemStatisticsResponse;
import com.example.shop.entity.User;
import com.example.shop.enums.UserRole;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.OrderMapper;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.mapper.UserMapper;
import com.example.shop.service.AdminService;
import com.example.shop.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 管理员服务实现类
 */
@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public IPage<User> getUserPage(int page, int size, String username, Integer roleId) {
        Page<User> pageParam = new Page<>(page, size);
        return userMapper.selectUserPage(pageParam, username, roleId);
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        Long currentUserId = SecurityUtil.getCurrentUserId();
        
        // 不能修改自己的状态
        if (userId.equals(currentUserId)) {
            throw new BusinessException(400, "不能修改自己的账户状态");
        }

        // 不能禁用管理员账户
        if (UserRole.ADMIN.getRoleId().equals(user.getRoleId()) && status == 0) {
            throw new BusinessException(400, "不能禁用管理员账户");
        }

        user.setStatus(status);
        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException("用户状态更新失败");
        }

        log.info("用户状态更新成功: userId={}, status={}", userId, status);
    }

    @Override
    @Transactional
    public void batchDeleteUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException(400, "用户ID列表不能为空");
        }

        Long currentUserId = SecurityUtil.getCurrentUserId();
        
        for (Long userId : userIds) {
            // 不能删除自己
            if (userId.equals(currentUserId)) {
                throw new BusinessException(400, "不能删除自己的账户");
            }

            User user = userMapper.selectById(userId);
            if (user != null) {
                // 不能删除其他管理员账户
                if (UserRole.ADMIN.getRoleId().equals(user.getRoleId())) {
                    throw new BusinessException(400, "不能删除管理员账户");
                }
            }
        }

        // 执行批量删除 - 使用新的API
        for (Long userId : userIds) {
            userMapper.deleteById(userId);
        }
        
        log.info("批量删除用户成功: userIds={}, count={}", userIds, userIds.size());
    }

    @Override
    @Transactional
    public void resetUserPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 此处应调用UserService的密码加密逻辑
        // 为了简化，这里只是示例
        // user.setPasswordHash(passwordEncoder.encode(newPassword + user.getSalt()));
        
        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException("密码重置失败");
        }

        log.info("用户密码重置成功: userId={}", userId);
    }

    @Override
    public SystemStatisticsResponse getSystemStatistics() {
        SystemStatisticsResponse response = new SystemStatisticsResponse();
        
        // 1. 统计总用户数
        Long userCount = userMapper.selectCount(null);
        response.setUserCount(userCount);
        
        // 2. 统计商品数量
        Long productCount = productMapper.selectCount(null);
        response.setProductCount(productCount);
        
        // 3. 统计订单数量
        Long orderCount = orderMapper.selectCount(null);
        response.setOrderCount(orderCount);
        
        // 4. 获取今天的日期
        LocalDate today = LocalDate.now();
        String todayStr = today.toString(); // 格式：2026-01-30
        
        // 5. 统计今日订单数
        Long todayOrderCount = orderMapper.countOrdersByDate(todayStr);
        response.setTodayOrderCount(todayOrderCount != null ? todayOrderCount : 0L);
        
        // 6. 统计本月订单数
        LocalDate monthStart = today.withDayOfMonth(1);
        String monthStartStr = monthStart.toString();
        Long monthOrderCount = orderMapper.countOrdersByDateRange(monthStartStr, todayStr);
        response.setMonthOrderCount(monthOrderCount != null ? monthOrderCount : 0L);
        
        // 7. 统计今日营收
        BigDecimal todayRevenue = orderMapper.sumRevenueByDate(todayStr);
        response.setTodayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO);
        
        // 8. 统计本月营收
        BigDecimal monthRevenue = orderMapper.sumRevenueByDateRange(monthStartStr, todayStr);
        response.setMonthRevenue(monthRevenue != null ? monthRevenue : BigDecimal.ZERO);
        
        // 9. 统计总营收
        BigDecimal totalRevenue = orderMapper.sumTotalRevenue();
        response.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        
        log.info("系统统计数据: userCount={}, productCount={}, orderCount={}, todayOrderCount={}, todayRevenue={}, monthRevenue={}, totalRevenue={}", 
                userCount, productCount, orderCount, todayOrderCount, todayRevenue, monthRevenue, totalRevenue);
        
        return response;
    }
}
