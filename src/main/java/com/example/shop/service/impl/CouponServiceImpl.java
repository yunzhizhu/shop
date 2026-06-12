package com.example.shop.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.constants.RedisConstants;
import com.example.shop.dto.*;
import com.example.shop.entity.Coupon;
import com.example.shop.entity.UserCoupon;
import com.example.shop.enums.CouponType;
import com.example.shop.enums.UserCouponStatus;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.CouponMapper;
import com.example.shop.mapper.UserCouponMapper;
import com.example.shop.service.CouponService;
import com.example.shop.service.RedisCacheService;
import com.example.shop.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 优惠券服务实现类
 * v2.0 添加Redis分布式锁，防止并发问题
 * v3.0 添加Redis缓存，提升查询性能
 */
@Slf4j
@Service
public class CouponServiceImpl implements CouponService {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    @Transactional
    public Long createCoupon(CouponCreateRequest request) {
        // 验证优惠券参数
        validateCouponRequest(request);

        // 创建优惠券
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(request, coupon);
        coupon.setReceivedCount(0);
        coupon.setStatus(1); // 默认启用

        int result = couponMapper.insert(coupon);
        if (result <= 0) {
            throw new BusinessException("优惠券创建失败");
        }

        // 清除可用优惠券列表缓存
        clearAvailableCouponsCache();

        log.info("优惠券创建成功: couponId={}, name={}", coupon.getCouponId(), coupon.getName());
        return coupon.getCouponId();
    }

    @Override
    @Transactional
    public void updateCoupon(CouponUpdateRequest request) {
        // 基本参数验证
        if (request.getCouponId() == null) {
            throw new BusinessException(400, "优惠券ID不能为空");
        }

        // 检查优惠券是否存在
        Coupon coupon = couponMapper.selectById(request.getCouponId());
        if (coupon == null) {
            throw new BusinessException(404, "优惠券不存在");
        }

        // 移除时间限制，允许修改已开始的优惠券
        // 管理员可以随时修改优惠券信息

        // 更新优惠券信息
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            coupon.setName(request.getName().trim());
        }
        if (request.getType() != null) {
            if (request.getType() < 1 || request.getType() > 3) {
                throw new BusinessException(400, "优惠券类型必须为1、2或3");
            }
            coupon.setType(request.getType());
        }
        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "优惠金额必须大于0");
            }
            coupon.setAmount(request.getAmount());
        }
        if (request.getMinPoint() != null) {
            if (request.getMinPoint().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(400, "使用门槛不能为负数");
            }
            coupon.setMinPoint(request.getMinPoint());
        }
        if (request.getStartTime() != null) {
            coupon.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            coupon.setEndTime(request.getEndTime());
        }
        if (request.getTotalCount() != null) {
            if (request.getTotalCount() <= 0) {
                throw new BusinessException(400, "发行数量必须大于0");
            }
            coupon.setTotalCount(request.getTotalCount());
        }
        if (request.getStatus() != null) {
            if (request.getStatus() != 0 && request.getStatus() != 1) {
                throw new BusinessException(400, "状态只能为0或1");
            }
            coupon.setStatus(request.getStatus());
        }

        // 满减券：优惠金额必须小于最低消费金额
        if (coupon.getType() != null && coupon.getType() == CouponType.FULL_REDUCTION.getCode()) {
            BigDecimal amount = coupon.getAmount();
            BigDecimal minPoint = coupon.getMinPoint();
            if (amount != null && minPoint != null && amount.compareTo(minPoint) >= 0) {
                throw new BusinessException(400, "优惠金额必须小于最低消费金额");
            }
        }

        // 验证更新后的数据
        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (!request.getStartTime().isBefore(request.getEndTime())) {
                throw new BusinessException(400, "开始时间必须早于结束时间");
            }
        } else if (request.getEndTime() != null) {
            if (!coupon.getStartTime().isBefore(request.getEndTime())) {
                throw new BusinessException(400, "开始时间必须早于结束时间");
            }
        } else if (request.getStartTime() != null) {
            if (!request.getStartTime().isBefore(coupon.getEndTime())) {
                throw new BusinessException(400, "开始时间必须早于结束时间");
            }
        }

        int result = couponMapper.updateById(coupon);
        if (result <= 0) {
            throw new BusinessException("优惠券更新失败");
        }

        // 清除优惠券缓存
        clearCouponCache(request.getCouponId());

        log.info("优惠券更新成功: couponId={}", request.getCouponId());
    }

    @Override
    public IPage<Coupon> getCouponPage(int page, int size, String name, Integer type, Integer status) {
        Page<Coupon> pageParam = new Page<>(page, size);
        return couponMapper.selectCouponPage(pageParam, name, type, status);
    }

    @Override
    public Coupon getCouponById(Long couponId) {
        String cacheKey = RedisConstants.getCouponInfoKey(couponId);

        // 1. 查Redis
        Coupon cached = redisCacheService.get(cacheKey, Coupon.class);
        if (cached != null) {
            // 检查是否是空对象（防止缓存穿透）
            if (cached.getCouponId() == null) {
                throw new BusinessException(404, "优惠券不存在");
            }
            log.debug("优惠券缓存命中: couponId={}", couponId);
            return cached;
        }

        // 2. 使用分布式锁（防止缓存击穿）
        String lockKey = RedisConstants.getCacheLoadLockKey("coupon", couponId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    // 双重检查
                    cached = redisCacheService.get(cacheKey, Coupon.class);
                    if (cached != null) {
                        if (cached.getCouponId() == null) {
                            throw new BusinessException(404, "优惠券不存在");
                        }
                        return cached;
                    }

                    // 3. 查MySQL
                    Coupon coupon = couponMapper.selectById(couponId);

                    if (coupon == null) {
                        // 缓存空对象（防止缓存穿透）
                        Coupon emptyCoupon = new Coupon();
                        redisCacheService.set(cacheKey, emptyCoupon,
                                RedisConstants.EMPTY_CACHE_TTL, TimeUnit.SECONDS);
                        log.debug("优惠券不存在，已缓存空对象: couponId={}", couponId);
                        throw new BusinessException(404, "优惠券不存在");
                    }

                    // 4. 缓存到Redis（随机TTL，防止缓存雪崩）
                    int ttl = RedisConstants.getRandomTtl(RedisConstants.COUPON_INFO_TTL);
                    redisCacheService.set(cacheKey, coupon, ttl, TimeUnit.SECONDS);
                    log.debug("优惠券已缓存: couponId={}, ttl={}秒", couponId, ttl);

                    return coupon;

                } finally {
                    lock.unlock();
                }
            } else {
                // 获取锁失败，等待50ms后重试读取缓存
                Thread.sleep(50);
                cached = redisCacheService.get(cacheKey, Coupon.class);
                if (cached != null && cached.getCouponId() != null) {
                    return cached;
                }
                throw new BusinessException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取优惠券被中断: couponId={}", couponId, e);
            throw new BusinessException("系统繁忙，请稍后重试");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取优惠券异常: couponId={}", couponId, e);
            throw new BusinessException("获取优惠券失败");
        }
    }

    @Override
    @Transactional
    public void disableCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(404, "优惠券不存在");
        }

        coupon.setStatus(0);
        int result = couponMapper.updateById(coupon);
        if (result <= 0) {
            throw new BusinessException("禁用优惠券失败");
        }

        // 清除优惠券缓存
        clearCouponCache(couponId);

        log.info("优惠券禁用成功: couponId={}", couponId);
    }

    @Override
    @Transactional
    public void enableCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(404, "优惠券不存在");
        }

        coupon.setStatus(1);
        int result = couponMapper.updateById(coupon);
        if (result <= 0) {
            throw new BusinessException("启用优惠券失败");
        }

        // 清除优惠券缓存
        clearCouponCache(couponId);

        log.info("优惠券启用成功: couponId={}", couponId);
    }

    @Override
    @Transactional
    public Integer toggleCouponStatus(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(404, "优惠券不存在");
        }

        // 切换状态：0->1, 1->0
        Integer newStatus = coupon.getStatus() == 1 ? 0 : 1;
        coupon.setStatus(newStatus);

        int result = couponMapper.updateById(coupon);
        if (result <= 0) {
            throw new BusinessException("切换优惠券状态失败");
        }

        // 清除优惠券缓存
        clearCouponCache(couponId);

        String action = newStatus == 1 ? "启用" : "禁用";
        log.info("优惠券状态切换成功: couponId={}, 新状态={} ({})", couponId, newStatus, action);

        return newStatus;
    }

    @Override
    public List<PublicCouponResponse> getAvailableCoupons(Long userId) {
        String cacheKey = RedisConstants.COUPON_AVAILABLE_LIST_KEY;

        // 1. 查Redis
        @SuppressWarnings("unchecked")
        List<Coupon> coupons = redisCacheService.get(cacheKey, List.class);

        if (coupons == null) {
            // 2. 缓存未命中，从数据库查询
            LocalDateTime currentTime = LocalDateTime.now();
            coupons = couponMapper.selectAvailableCoupons(currentTime);

            // 3. 缓存到Redis（随机TTL，防止缓存雪崩，即使是空列表也缓存）
            int ttl = RedisConstants.getRandomTtl(RedisConstants.COUPON_AVAILABLE_LIST_TTL);
            redisCacheService.set(cacheKey, coupons, ttl, TimeUnit.SECONDS);
            log.debug("可用优惠券列表已缓存，数量: {}, ttl={}秒", coupons.size(), ttl);
        } else {
            log.debug("可用优惠券列表缓存命中，数量: {}", coupons.size());
            // 空列表也直接返回（防止缓存穿透）
            if (coupons.isEmpty()) {
                return new ArrayList<>();
            }
        }

        return coupons.stream().map(coupon -> {
            PublicCouponResponse response = new PublicCouponResponse();
            BeanUtils.copyProperties(coupon, response);

            // 设置类型描述
            CouponType couponType = CouponType.getByCode(coupon.getType());
            response.setTypeDesc(couponType != null ? couponType.getDescription() : "未知类型");

            // 计算剩余数量
            response.setRemaining(coupon.getTotalCount() - coupon.getReceivedCount());

            // 检查是否已领取
            if (userId != null) {
                UserCoupon userCoupon = userCouponMapper.selectByUserIdAndCouponId(userId, coupon.getCouponId());
                response.setReceived(userCoupon != null);
            } else {
                response.setReceived(false);
            }

            return response;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long receiveCoupon(Long couponId) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 使用分布式锁防止并发超发
        String lockKey = RedisConstants.getCouponReceiveLockKey(couponId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待5秒，持有10秒
            if (lock.tryLock(RedisConstants.LOCK_WAIT_TIME, RedisConstants.LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    LocalDateTime currentTime = LocalDateTime.now();

                    // 检查优惠券是否存在且可领取
                    Coupon coupon = couponMapper.selectById(couponId);
                    if (coupon == null) {
                        throw new BusinessException(404, "优惠券不存在");
                    }

                    if (coupon.getStatus() != 1) {
                        throw new BusinessException(400, "优惠券已禁用");
                    }

                    if (currentTime.isBefore(coupon.getStartTime()) || currentTime.isAfter(coupon.getEndTime())) {
                        throw new BusinessException(400, "优惠券不在有效期内");
                    }

                    if (coupon.getReceivedCount() >= coupon.getTotalCount()) {
                        throw new BusinessException(400, "优惠券已领完");
                    }

                    // 检查用户是否已领取
                    UserCoupon existingUserCoupon = userCouponMapper.selectByUserIdAndCouponId(userId, couponId);
                    if (existingUserCoupon != null) {
                        throw new BusinessException(400, "您已领取过该优惠券");
                    }

                    // 创建用户优惠券记录
                    UserCoupon userCoupon = new UserCoupon();
                    userCoupon.setUserId(userId);
                    userCoupon.setCouponId(couponId);
                    userCoupon.setStatus(UserCouponStatus.UNUSED.getCode());
                    userCoupon.setGetTime(currentTime);

                    int result = userCouponMapper.insert(userCoupon);
                    if (result <= 0) {
                        throw new BusinessException("领取优惠券失败");
                    }

                    // 增加已领取数量
                    couponMapper.increaseReceivedCount(couponId);

                    // 清除相关缓存
                    clearCouponCache(couponId);
                    clearUserCouponsCache(userId);

                    log.info("用户领取优惠券成功: userId={}, couponId={}, userCouponId={}",
                            userId, couponId, userCoupon.getUserCouponId());
                    return userCoupon.getUserCouponId();

                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取优惠券领取锁超时: couponId={}, userId={}", couponId, userId);
                throw new BusinessException("系统繁忙，请稍后再试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("领取优惠券被中断: couponId={}, userId={}", couponId, userId, e);
            throw new BusinessException("领取优惠券失败");
        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            log.error("领取优惠券异常: couponId={}, userId={}", couponId, userId, e);
            throw new BusinessException("领取优惠券失败");
        }
    }

    @Override
    public List<UserCouponResponse> getUserCoupons(Integer status) {
        Long userId = SecurityUtil.getCurrentUserId();

        String cacheKey = RedisConstants.getUserCouponsKey(userId, status);

        // 1. 查Redis
        @SuppressWarnings("unchecked")
        List<UserCouponResponse> userCoupons = redisCacheService.get(cacheKey, List.class);

        if (userCoupons == null) {
            // 2. 缓存未命中，从数据库查询
            userCoupons = userCouponMapper.selectUserCoupons(userId, status);

            // 3. 缓存到Redis（随机TTL，防止缓存雪崩，即使是空列表也缓存）
            int ttl = RedisConstants.getRandomTtl(RedisConstants.USER_COUPONS_TTL);
            redisCacheService.set(cacheKey, userCoupons, ttl, TimeUnit.SECONDS);
            log.debug("用户优惠券列表已缓存: userId={}, status={}, 数量={}, ttl={}秒",
                    userId, status, userCoupons.size(), ttl);
        } else {
            log.debug("用户优惠券列表缓存命中: userId={}, status={}, 数量={}",
                    userId, status, userCoupons.size());
            // 空列表也直接返回（防止缓存穿透）
            if (userCoupons.isEmpty()) {
                return new ArrayList<>();
            }
        }

        // 设置描述信息
        userCoupons.forEach(userCoupon -> {
            CouponType couponType = CouponType.getByCode(userCoupon.getType());
            userCoupon.setTypeDesc(couponType != null ? couponType.getDescription() : "未知类型");

            UserCouponStatus couponStatus = UserCouponStatus.getByCode(userCoupon.getStatus());
            userCoupon.setStatusDesc(couponStatus != null ? couponStatus.getDescription() : "未知状态");
        });

        return userCoupons;
    }

    @Override
    public List<AvailableCouponResponse> getAvailableCoupons(BigDecimal orderAmount) {
        Long userId = SecurityUtil.getCurrentUserId();
        LocalDateTime currentTime = LocalDateTime.now();

        List<UserCouponResponse> userCoupons = userCouponMapper.selectAvailableUserCoupons(userId, currentTime);

        return userCoupons.stream()
                .filter(userCoupon -> {
                    CouponType couponType = CouponType.getByCode(userCoupon.getType());
                    return couponType != null && couponType.canUse(userCoupon.getMinPoint(), orderAmount);
                })
                .map(userCoupon -> {
                    AvailableCouponResponse response = new AvailableCouponResponse();
                    BeanUtils.copyProperties(userCoupon, response);

                    // 计算实际优惠金额
                    CouponType couponType = CouponType.getByCode(userCoupon.getType());
                    BigDecimal discountAmount = couponType.calculateDiscount(userCoupon.getAmount(), orderAmount);
                    response.setDiscountAmount(discountAmount);

                    // 设置到期时间
                    response.setEndTime(userCoupon.getEndTime());

                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void useCoupon(Long userCouponId, Long orderId) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 使用分布式锁防止重复使用
        String lockKey = RedisConstants.getUserCouponLockKey(userCouponId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待5秒，持有10秒
            if (lock.tryLock(RedisConstants.LOCK_WAIT_TIME, RedisConstants.LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    // 检查用户优惠券是否存在且属于当前用户
                    UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
                    if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
                        throw new BusinessException(404, "优惠券不存在");
                    }

                    // 检查优惠券状态
                    if (!UserCouponStatus.UNUSED.getCode().equals(userCoupon.getStatus())) {
                        throw new BusinessException(400, "优惠券不可用");
                    }

                    // 使用优惠券
                    int result = userCouponMapper.useCoupon(userCouponId, LocalDateTime.now());
                    if (result <= 0) {
                        throw new BusinessException("使用优惠券失败");
                    }

                    // 清除用户优惠券缓存
                    clearUserCouponsCache(userId);

                    log.info("优惠券使用成功: userCouponId={}, orderId={}, userId={}",
                            userCouponId, orderId, userId);

                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取优惠券使用锁超时: userCouponId={}, userId={}", userCouponId, userId);
                throw new BusinessException("系统繁忙，请稍后再试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("使用优惠券被中断: userCouponId={}, userId={}", userCouponId, userId, e);
            throw new BusinessException("使用优惠券失败");
        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            log.error("使用优惠券异常: userCouponId={}, userId={}", userCouponId, userId, e);
            throw new BusinessException("使用优惠券失败");
        }
    }

    @Override
    @Transactional
    public void returnCoupon(Long userCouponId) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 检查用户优惠券是否存在且属于当前用户
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(404, "优惠券不存在");
        }

        // 检查优惠券状态
        if (!UserCouponStatus.USED.getCode().equals(userCoupon.getStatus())) {
            throw new BusinessException(400, "优惠券状态不正确");
        }

        // 退还优惠券
        int result = userCouponMapper.returnCoupon(userCouponId);
        if (result <= 0) {
            throw new BusinessException("退还优惠券失败");
        }

        // 清除用户优惠券缓存
        clearUserCouponsCache(userId);

        log.info("优惠券退还成功: userCouponId={}", userCouponId);
    }

    /**
     * 系统内部退还优惠券（用于订单超时取消等场景，不需要用户上下文）
     */
    @Transactional
    public void returnCouponInternal(Long userCouponId) {
        // 检查用户优惠券是否存在
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            log.warn("优惠券不存在，无法退还: userCouponId={}", userCouponId);
            return;
        }

        // 检查优惠券状态
        if (!UserCouponStatus.USED.getCode().equals(userCoupon.getStatus())) {
            log.warn("优惠券状态不正确，无法退还: userCouponId={}, status={}",
                    userCouponId, userCoupon.getStatus());
            return;
        }

        // 退还优惠券
        int result = userCouponMapper.returnCoupon(userCouponId);
        if (result <= 0) {
            log.error("退还优惠券失败: userCouponId={}", userCouponId);
            throw new BusinessException("退还优惠券失败");
        }

        // 清除用户优惠券缓存
        clearUserCouponsCache(userCoupon.getUserId());

        log.info("系统内部退还优惠券成功: userCouponId={}, userId={}",
                userCouponId, userCoupon.getUserId());
    }

    @Override
    public BigDecimal calculateDiscount(Long userCouponId, BigDecimal orderAmount) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            return BigDecimal.ZERO;
        }

        CouponType couponType = CouponType.getByCode(coupon.getType());
        if (couponType == null) {
            return BigDecimal.ZERO;
        }

        return couponType.calculateDiscount(coupon.getAmount(), orderAmount);
    }

    @Override
    public boolean canUseCoupon(Long userCouponId, BigDecimal orderAmount) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            return false;
        }

        // 检查状态
        if (!UserCouponStatus.UNUSED.getCode().equals(userCoupon.getStatus())) {
            return false;
        }

        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            return false;
        }

        // 检查时间
        LocalDateTime currentTime = LocalDateTime.now();
        if (currentTime.isBefore(coupon.getStartTime()) || currentTime.isAfter(coupon.getEndTime())) {
            return false;
        }

        // 检查使用条件
        CouponType couponType = CouponType.getByCode(coupon.getType());
        return couponType != null && couponType.canUse(coupon.getMinPoint(), orderAmount);
    }

    @Override
    @Transactional
    public void updateExpiredCoupons() {
        LocalDateTime currentTime = LocalDateTime.now();
        int count = userCouponMapper.updateExpiredCoupons(currentTime);
        log.info("更新过期优惠券数量: {}", count);
    }

    /**
     * 验证优惠券创建请求
     */
    private void validateCouponRequest(CouponCreateRequest request) {
        // 验证时间
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessException(400, "开始时间必须早于结束时间");
        }

        // 验证优惠券类型和金额
        CouponType couponType = CouponType.getByCode(request.getType());
        if (couponType == null) {
            throw new BusinessException(400, "无效的优惠券类型");
        }

        if (couponType == CouponType.DISCOUNT) {
            // 折扣券的amount必须在(0,100]区间
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0 ||
                request.getAmount().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException(400, "折扣率必须在0-100之间");
            }
        }

        // 满减券必须有使用门槛
        if (couponType == CouponType.FULL_REDUCTION && request.getMinPoint() == null) {
            throw new BusinessException(400, "满减券必须设置使用门槛");
        }

        // 满减券：优惠金额必须小于最低消费金额
        if (couponType == CouponType.FULL_REDUCTION && request.getMinPoint() != null) {
            if (request.getAmount().compareTo(request.getMinPoint()) >= 0) {
                throw new BusinessException(400, "优惠金额必须小于最低消费金额");
            }
        }
    }

    /**
     * 清除优惠券缓存
     */
    private void clearCouponCache(Long couponId) {
        // 清除优惠券详情缓存
        String couponInfoKey = RedisConstants.getCouponInfoKey(couponId);
        redisCacheService.delete(couponInfoKey);

        // 清除可用优惠券列表缓存
        clearAvailableCouponsCache();

        log.debug("优惠券缓存已清除: couponId={}", couponId);
    }

    /**
     * 清除可用优惠券列表缓存
     */
    private void clearAvailableCouponsCache() {
        redisCacheService.delete(RedisConstants.COUPON_AVAILABLE_LIST_KEY);
        log.debug("可用优惠券列表缓存已清除");
    }

    /**
     * 清除用户优惠券缓存
     */
    private void clearUserCouponsCache(Long userId) {
        // 清除所有状态的用户优惠券缓存
        redisCacheService.delete(RedisConstants.getUserCouponsKey(userId, null));
        redisCacheService.delete(RedisConstants.getUserCouponsKey(userId, 0)); // 未使用
        redisCacheService.delete(RedisConstants.getUserCouponsKey(userId, 1)); // 已使用
        redisCacheService.delete(RedisConstants.getUserCouponsKey(userId, 2)); // 已过期

        log.debug("用户优惠券缓存已清除: userId={}", userId);
    }
}
