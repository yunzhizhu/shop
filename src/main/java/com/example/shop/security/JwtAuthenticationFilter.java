package com.example.shop.security;

import com.example.shop.config.JwtProperties;
import com.example.shop.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String token = getTokenFromRequest(request);
        
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwtUtil.getUsernameFromToken(token);
                
                if (StringUtils.hasText(username) && jwtUtil.validateToken(token, username)) {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);
                    
                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        );

                    // 创建自定义的Details对象来存储用户ID
                    Map<String, Object> details = new HashMap<>();
                    details.put("userId", userId);
                    details.put("request", new WebAuthenticationDetailsSource().buildDetails(request));
                    authentication.setDetails(details);
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("JWT认证成功: username={}, role={}", username, role);
                }
            } catch (Exception e) {
                log.error("JWT认证失败: {}", e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.getHeader());
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtProperties.getPrefix() + " ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
