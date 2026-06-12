package com.example.shop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
@AllArgsConstructor
public enum UserRole {
    ADMIN(1, "admin", "系统管理员"),
    USER(2, "user", "普通用户"),
    SUPER(3, "super", "超级审核员");

    private final Integer roleId;
    private final String roleName;
    private final String description;

    /**
     * 根据角色ID获取角色枚举
     */
    public static UserRole getByRoleId(Integer roleId) {
        for (UserRole role : values()) {
            if (role.getRoleId().equals(roleId)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 根据角色名称获取角色枚举
     */
    public static UserRole getByRoleName(String roleName) {
        for (UserRole role : values()) {
            if (role.getRoleName().equals(roleName)) {
                return role;
            }
        }
        return null;
    }
}
