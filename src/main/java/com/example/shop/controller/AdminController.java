package com.example.shop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.dto.BatchDeleteRequest;
import com.example.shop.dto.SystemStatisticsResponse;
import com.example.shop.entity.User;
import com.example.shop.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public Result<IPage<User>> getUsers(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String username,
                                       @RequestParam(required = false) Integer roleId) {
        IPage<User> userPage = adminService.getUserPage(page, size, username, roleId);
        return Result.success(userPage);
    }

    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/users/{userId}")
    public Result<User> getUserById(@PathVariable Long userId) {
        User user = adminService.getUserById(userId);
        if (user == null) {
            return Result.notFound("用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/users/{userId}/status")
    @SystemLog(operation = "更新用户状态", module = "管理员模块", action = "updateUserStatus")
    public Result<Void> updateUserStatus(@PathVariable Long userId, 
                                        @RequestParam Integer status) {
        adminService.updateUserStatus(userId, status);
        return Result.success("用户状态更新成功", null);
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/users")
    @SystemLog(operation = "批量删除用户", module = "管理员模块", action = "batchDeleteUsers")
    public Result<Void> batchDeleteUsers(@Valid @RequestBody BatchDeleteRequest request) {
        adminService.batchDeleteUsers(request.getUserIds());
        return Result.success("删除成功", null);
    }

    /**
     * 获取系统统计数据
     */
    @GetMapping("/statistics")
    public Result<SystemStatisticsResponse> getSystemStatistics() {
        SystemStatisticsResponse statistics = adminService.getSystemStatistics();
        return Result.success(statistics);
    }
}
