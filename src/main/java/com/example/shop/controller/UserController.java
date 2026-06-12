package com.example.shop.controller;

import com.example.shop.annotation.SystemLog;
import com.example.shop.common.Result;
import com.example.shop.dto.*;
import com.example.shop.entity.UserAddress;
import com.example.shop.service.UserAddressService;
import com.example.shop.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserAddressService userAddressService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public Result<UserProfileResponse> getProfile() {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        return Result.success(profile);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    @SystemLog(operation = "更新用户信息", module = "用户模块", action = "updateProfile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(request);
        return Result.success("更新成功", null);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @SystemLog(operation = "修改密码", module = "用户模块", action = "changePassword")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return Result.success("密码修改成功", null);
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    @SystemLog(operation = "上传头像", module = "用户模块", action = "uploadAvatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(file);
        
        Map<String, String> data = new HashMap<>();
        data.put("avatarUrl", avatarUrl);
        
        return Result.success("头像上传成功", data);
    }

    /**
     * 获取用户地址列表
     */
    @GetMapping("/address")
    public Result<List<UserAddress>> getAddresses() {
        List<UserAddress> addresses = userAddressService.getUserAddresses();
        return Result.success(addresses);
    }

    /**
     * 添加地址
     */
    @PostMapping("/address")
    @SystemLog(operation = "添加地址", module = "用户模块", action = "addAddress")
    public Result<Map<String, Long>> addAddress(@Valid @RequestBody AddAddressRequest request) {
        Long addressId = userAddressService.addAddress(request);
        
        Map<String, Long> data = new HashMap<>();
        data.put("addressId", addressId);
        
        return Result.success("地址添加成功", data);
    }

    /**
     * 更新地址
     */
    @PutMapping("/address/{addressId}")
    @SystemLog(operation = "更新地址", module = "用户模块", action = "updateAddress")
    public Result<Void> updateAddress(@PathVariable Long addressId, 
                                    @Valid @RequestBody AddAddressRequest request) {
        userAddressService.updateAddress(addressId, request);
        return Result.success("地址更新成功", null);
    }

    /**
     * 删除地址
     */
    @DeleteMapping("/address/{addressId}")
    @SystemLog(operation = "删除地址", module = "用户模块", action = "deleteAddress")
    public Result<Void> deleteAddress(@PathVariable Long addressId) {
        userAddressService.deleteAddress(addressId);
        return Result.success("地址删除成功", null);
    }

    /**
     * 设置默认地址
     */
    @PutMapping("/address/{addressId}/default")
    @SystemLog(operation = "设置默认地址", module = "用户模块", action = "setDefaultAddress")
    public Result<Void> setDefaultAddress(@PathVariable Long addressId) {
        userAddressService.setDefaultAddress(addressId);
        return Result.success("设置默认地址成功", null);
    }

    /**
     * 获取默认地址
     */
    @GetMapping("/address/default")
    public Result<UserAddress> getDefaultAddress() {
        UserAddress defaultAddress = userAddressService.getDefaultAddress();
        return Result.success(defaultAddress);
    }
}
