package com.example.shop.dto;

import lombok.Data;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新用户信息请求DTO
 */
@Data
public class UpdateProfileRequest {

    /**
     * 用户名
     */
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    private String phone;
}
