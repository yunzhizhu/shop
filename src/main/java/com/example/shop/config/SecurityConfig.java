package com.example.shop.config;

import com.example.shop.security.JwtAuthenticationFilter;
import com.example.shop.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置类
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("!test")  // 在测试环境中不加载此配置
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 认证提供者
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF
            .csrf(csrf -> csrf.disable())

            // 配置CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 配置会话管理
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 配置授权规则
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/category/**").permitAll()
                
                // WebSocket端点
                .requestMatchers("/ws-native/**").permitAll()
                .requestMatchers("/ws-message/**").permitAll()
                .requestMatchers("/ws-message-sockjs/**").permitAll()

                // 静态资源
                .requestMatchers("/api-test.html").permitAll()
                .requestMatchers("/index.html").permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("/static/**").permitAll()
                .requestMatchers("/css/**").permitAll()
                .requestMatchers("/js/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/favicon.ico").permitAll()

                // 商品公开接口（未登录用户也可以访问）
                .requestMatchers("/product/list").permitAll()
                .requestMatchers("/product/search").permitAll()
                .requestMatchers("/product/detail/**").permitAll()
                .requestMatchers("/product/**").permitAll()

                // 评价公开接口
                .requestMatchers("/review/list").permitAll()

                // 优惠券公开接口
                .requestMatchers("/coupon/list").permitAll()

                // 购物车公开接口（未登录用户可以访问）
                .requestMatchers("/cart/count").permitAll()

                // Redis测试接口（开发环境使用）
                .requestMatchers("/test/redis/**").permitAll()
                
                // WebSocket调试接口（开发环境使用）
                .requestMatchers("/websocket-debug/**").permitAll()

                // 管理员接口
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Super 审核员接口
                .requestMatchers("/super/**").hasRole("SUPER")

                // 其他接口需要认证
                .anyRequest().authenticated()
            )

            // 配置认证提供者
            .authenticationProvider(authenticationProvider())

            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
