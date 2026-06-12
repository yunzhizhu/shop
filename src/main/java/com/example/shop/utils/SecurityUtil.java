package com.example.shop.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 */
public class SecurityUtil {

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> details = (java.util.Map<String, Object>) authentication.getDetails();
            return (Long) details.get("userId");
        }
        return null;
    }

    /**
     * 检查当前用户是否有指定角色
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.toUpperCase()));
        }
        return false;
    }

    /**
     * 检查当前用户是否为管理员
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * 获取当前用户角色
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .map(authority -> authority.substring(5).toLowerCase())
                    .findFirst()
                    .orElse("user");
        }
        return "user";
    }
}
