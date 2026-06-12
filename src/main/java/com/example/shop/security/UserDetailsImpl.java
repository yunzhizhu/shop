package com.example.shop.security;

import com.example.shop.entity.User;
import com.example.shop.enums.UserRole;
import com.example.shop.enums.UserStatus;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security用户详情实现
 */
@Data
public class UserDetailsImpl implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private String role;
    private Integer status;

    public UserDetailsImpl(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.status = user.getStatus();
        
        // 设置角色
        UserRole userRole = UserRole.getByRoleId(user.getRoleId());
        this.role = userRole != null ? userRole.getRoleName() : "user";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return UserStatus.NORMAL.getCode().equals(status);
    }
}
