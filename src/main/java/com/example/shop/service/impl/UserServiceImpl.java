package com.example.shop.service.impl;

import com.example.shop.constants.RedisConstants;
import com.example.shop.dto.ChangePasswordRequest;
import com.example.shop.dto.UpdateProfileRequest;
import com.example.shop.dto.UserProfileResponse;
import com.example.shop.entity.User;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.UserMapper;
import com.example.shop.service.RedisCacheService;
import com.example.shop.service.UserService;
import com.example.shop.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 * v2.0 添加Redis缓存支持
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.example.shop.helper.FileUploadHelper fileUploadHelper;
    
    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public UserProfileResponse getCurrentUserProfile() {
        Long userId = SecurityUtil.getCurrentUserId();
        String cacheKey = RedisConstants.getUserInfoKey(userId);
        
        // 1. 尝试从Redis获取缓存
        UserProfileResponse cached = redisCacheService.get(cacheKey, UserProfileResponse.class);
        if (cached != null) {
            // 检查是否是空对象（防止缓存穿透）
            if (cached.getUserId() == null) {
                log.debug("用户不存在（空对象缓存）: userId={}", userId);
                throw new BusinessException(404, "用户不存在");
            }
            log.debug("用户信息缓存命中: userId={}", userId);
            return cached;
        }

        // 2. 缓存未命中，使用分布式锁防止缓存击穿
        String lockKey = RedisConstants.getCacheLoadLockKey("user", userId);
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试获取锁，等待3秒，持有10秒
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    // 双重检查：获取锁后再次查询缓存
                    cached = redisCacheService.get(cacheKey, UserProfileResponse.class);
                    if (cached != null) {
                        if (cached.getUserId() == null) {
                            throw new BusinessException(404, "用户不存在");
                        }
                        return cached;
                    }
                    
                    // 3. 从数据库查询
                    User user = userMapper.selectById(userId);
                    if (user == null) {
                        // 缓存空对象，防止缓存穿透（5分钟TTL）
                        UserProfileResponse emptyResponse = new UserProfileResponse();
                        redisCacheService.set(cacheKey, emptyResponse, 
                                RedisConstants.EMPTY_CACHE_TTL, TimeUnit.SECONDS);
                        log.info("用户不存在，已缓存空对象: userId={}", userId);
                        throw new BusinessException(404, "用户不存在");
                    }

                    // 4. 构建响应对象
                    UserProfileResponse response = new UserProfileResponse();
                    BeanUtils.copyProperties(user, response);
                    
                    // 转换头像为完整URL
                    if (response.getAvatar() != null && !response.getAvatar().isEmpty()) {
                        response.setAvatar(fileUploadHelper.toFullUrl(response.getAvatar()));
                    }
                    
                    // 设置角色信息
                    com.example.shop.enums.UserRole userRole = com.example.shop.enums.UserRole.getByRoleId(user.getRoleId());
                    if (userRole != null) {
                        response.setRole(userRole.getRoleName());
                        response.setRoleDescription(userRole.getDescription());
                    }
                    
                    // 5. 缓存到Redis（带随机TTL，防止缓存雪崩）
                    int ttl = RedisConstants.getRandomTtl(RedisConstants.USER_INFO_TTL);
                    redisCacheService.set(cacheKey, response, ttl, TimeUnit.SECONDS);
                    log.debug("用户信息已缓存: userId={}, ttl={}秒", userId, ttl);
                    
                    return response;
                    
                } finally {
                    lock.unlock();
                }
            } else {
                // 获取锁失败，等待一下再查缓存
                Thread.sleep(50);
                cached = redisCacheService.get(cacheKey, UserProfileResponse.class);
                if (cached != null && cached.getUserId() != null) {
                    return cached;
                }
                throw new BusinessException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取用户信息被中断: userId={}", userId, e);
            throw new BusinessException("系统繁忙，请稍后重试");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户信息异常: userId={}", userId, e);
            throw new BusinessException("获取用户信息失败");
        }
    }
    
    /**
     * 清除用户缓存
     */
    private void clearUserCache(Long userId) {
        redisCacheService.delete(RedisConstants.getUserInfoKey(userId));
        log.debug("已清除用户缓存: userId={}", userId);
    }

    @Override
    @Transactional
    public void updateProfile(UpdateProfileRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 检查用户名是否已被其他用户使用
        if (StringUtils.hasText(request.getUsername()) && !request.getUsername().equals(user.getUsername())) {
            User existingUser = userMapper.selectByUsername(request.getUsername());
            if (existingUser != null && !existingUser.getUserId().equals(userId)) {
                throw new BusinessException(1001, "用户名已存在");
            }
            user.setUsername(request.getUsername());
        }

        // 检查手机号是否已被其他用户使用
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            User existingUser = userMapper.selectByPhone(request.getPhone());
            if (existingUser != null && !existingUser.getUserId().equals(userId)) {
                throw new BusinessException(1002, "手机号已被使用");
            }
            user.setPhone(request.getPhone());
        }

        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException("更新失败");
        }

        log.info("用户信息更新成功: userId={}", userId);
        
        // 清除用户缓存
        clearUserCache(userId);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword() + user.getSalt(), user.getPasswordHash())) {
            throw new BusinessException(400, "旧密码错误");
        }

        // 生成新的盐值和密码哈希
        String newSalt = generateSalt();
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword() + newSalt);

        user.setSalt(newSalt);
        user.setPasswordHash(newPasswordHash);

        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException("密码修改失败");
        }

        log.info("用户密码修改成功: userId={}", userId);
    }

    @Override
    @Transactional
    public String uploadAvatar(MultipartFile file) {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 使用FileUploadHelper验证文件
        fileUploadHelper.validateFile(file);

        try {
            // 生成文件名
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString() + "." + fileExtension;

            // 创建上传目录
            fileUploadHelper.createDirectoryIfNotExists(fileUploadHelper.getAvatarPath());

            // 保存文件
            Path filePath = fileUploadHelper.buildFilePath(fileUploadHelper.getAvatarPath(), fileName);
            Files.copy(file.getInputStream(), filePath);

            // 构建访问URL（相对路径）
            String avatarUrl = fileUploadHelper.getAvatarAccessUrl(fileName);

            // 更新用户头像（数据库存储相对路径）
            user.setAvatar(avatarUrl);
            int result = userMapper.updateById(user);
            if (result <= 0) {
                throw new BusinessException("头像更新失败");
            }

            // 返回完整URL给前端
            String fullAvatarUrl = fileUploadHelper.toFullUrl(avatarUrl);
            log.info("用户头像上传成功: userId={}, avatarUrl={}", userId, fullAvatarUrl);
            
            // 清除用户缓存
            clearUserCache(userId);
            
            return fullAvatarUrl;

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    @Transactional
    public void updateBalance(Long userId, BigDecimal amount) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        BigDecimal newBalance = user.getBalance().add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(1003, "余额不足");
        }

        user.setBalance(newBalance);
        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException("余额更新失败");
        }

        // 清除用户缓存
        clearUserCache(userId);

        log.info("用户余额更新成功: userId={}, amount={}, newBalance={}", userId, amount, newBalance);
    }

    @Override
    @Transactional
    public void updatePoints(Long userId, Integer points) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        Integer newPoints = user.getPoints() + points;
        if (newPoints < 0) {
            throw new BusinessException("积分不足");
        }

        user.setPoints(newPoints);
        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException("积分更新失败");
        }

        // 清除用户缓存
        clearUserCache(userId);

        log.info("用户积分更新成功: userId={}, points={}, newPoints={}", userId, points, newPoints);
    }

    /**
     * 生成盐值
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 检查是否为图片文件
     */
    private boolean isImageFile(String filename) {
        String extension = getFileExtension(filename);
        return fileUploadHelper.isAllowedExtension(extension);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
