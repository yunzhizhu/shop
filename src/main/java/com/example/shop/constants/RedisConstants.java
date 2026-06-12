package com.example.shop.constants;

/**
 * Redis键常量类
 * 统一管理所有Redis键的命名规范
 * v2.0 添加shopping前缀，与MySQL数据库名保持一致
 */
public class RedisConstants {

    // ==================== 数据库前缀 ====================
    
    /**
     * 数据库前缀：shopping:
     * 与MySQL数据库名保持一致
     */
    public static final String DB_PREFIX = "shopping:";

    // ==================== 商品相关缓存 ====================
    
    /**
     * 商品信息缓存：shopping:product:info:{productId}
     * 过期时间：30分钟
     */
    public static final String PRODUCT_INFO_KEY = DB_PREFIX + "product:info:";
    public static final long PRODUCT_INFO_TTL = 30 * 60; // 30分钟
    
    /**
     * 商品图片列表缓存：shopping:product:images:{productId}
     * 过期时间：1小时
     */
    public static final String PRODUCT_IMAGES_KEY = DB_PREFIX + "product:images:";
    public static final long PRODUCT_IMAGES_TTL = 60 * 60; // 1小时
    
    /**
     * 商品列表缓存：shopping:product:list:{categoryId}:{page}:{size}
     * 过期时间：10分钟
     */
    public static final String PRODUCT_LIST_KEY = DB_PREFIX + "product:list:";
    public static final long PRODUCT_LIST_TTL = 10 * 60; // 10分钟
    
    /**
     * 热门商品列表缓存：shopping:product:hot:list
     * 过期时间：30分钟
     */
    public static final String PRODUCT_HOT_LIST_KEY = DB_PREFIX + "product:hot:list";
    public static final long PRODUCT_HOT_LIST_TTL = 30 * 60; // 30分钟

    // ==================== 分类相关缓存 ====================
    
    /**
     * 分类信息缓存：shopping:category:info:{categoryId}
     * 过期时间：1天
     */
    public static final String CATEGORY_INFO_KEY = DB_PREFIX + "category:info:";
    public static final long CATEGORY_INFO_TTL = 24 * 60 * 60; // 1天
    
    /**
     * 所有分类列表缓存：shopping:category:list:all
     * 过期时间：1天
     */
    public static final String CATEGORY_LIST_ALL_KEY = DB_PREFIX + "category:list:all";
    public static final long CATEGORY_LIST_ALL_TTL = 24 * 60 * 60; // 1天
    
    /**
     * 启用的分类列表缓存：shopping:category:list:enabled
     * 过期时间：1天
     */
    public static final String CATEGORY_LIST_ENABLED_KEY = DB_PREFIX + "category:list:enabled";
    public static final long CATEGORY_LIST_ENABLED_TTL = 24 * 60 * 60; // 1天

    // ==================== 用户相关缓存 ====================
    
    /**
     * 用户信息缓存：shopping:user:info:{userId}
     * 过期时间：1小时
     */
    public static final String USER_INFO_KEY = DB_PREFIX + "user:info:";
    public static final long USER_INFO_TTL = 60 * 60; // 1小时
    
    /**
     * 用户收藏列表缓存：shopping:user:favorites:{userId}
     * 过期时间：10分钟
     */
    public static final String USER_FAVORITES_KEY = DB_PREFIX + "user:favorites:";
    public static final long USER_FAVORITES_TTL = 10 * 60; // 10分钟

    // ==================== 评论相关缓存 ====================
    
    /**
     * 商品评论列表缓存：shopping:review:list:{productId}:{page}
     * 过期时间：10分钟
     */
    public static final String REVIEW_LIST_KEY = DB_PREFIX + "review:list:";
    public static final long REVIEW_LIST_TTL = 10 * 60; // 10分钟
    
    /**
     * 商品评论统计缓存：shopping:review:stats:{productId}
     * 过期时间：30分钟
     */
    public static final String REVIEW_STATS_KEY = DB_PREFIX + "review:stats:";
    public static final long REVIEW_STATS_TTL = 30 * 60; // 30分钟

    // ==================== 优惠券相关缓存 ====================
    
    /**
     * 优惠券信息缓存：shopping:coupon:info:{couponId}
     * 过期时间：1小时
     */
    public static final String COUPON_INFO_KEY = DB_PREFIX + "coupon:info:";
    public static final long COUPON_INFO_TTL = 60 * 60; // 1小时
    
    /**
     * 可用优惠券列表缓存：shopping:coupon:available:list
     * 过期时间：10分钟
     */
    public static final String COUPON_AVAILABLE_LIST_KEY = DB_PREFIX + "coupon:available:list";
    public static final long COUPON_AVAILABLE_LIST_TTL = 10 * 60; // 10分钟
    
    /**
     * 用户优惠券列表缓存：shopping:user:coupons:{userId}:{status}
     * 过期时间：5分钟
     */
    public static final String USER_COUPONS_KEY = DB_PREFIX + "user:coupons:";
    public static final long USER_COUPONS_TTL = 5 * 60; // 5分钟

    // ==================== 购物车缓存 ====================
    
    /**
     * 购物车缓存：shopping:cart:user:{userId}
     * 过期时间：7天
     */
    public static final String CART_KEY = DB_PREFIX + "cart:user:";
    public static final long CART_TTL = 7 * 24 * 60 * 60; // 7天

    // ==================== 库存相关键 ====================
    
    /**
     * 商品库存：shopping:stock:product:{productId}
     * 存储商品的总库存数量
     */
    public static final String STOCK_KEY = DB_PREFIX + "stock:product:";
    
    /**
     * 预占库存：shopping:reserved:product:{productId}
     * 存储商品的预占库存数量
     */
    public static final String RESERVED_STOCK_KEY = DB_PREFIX + "reserved:product:";
    
    /**
     * 库存锁：shopping:lock:stock:{productId}
     * 用于库存扣减时的分布式锁
     */
    public static final String STOCK_LOCK_KEY = DB_PREFIX + "lock:stock:";

    // ==================== 订单相关键 ====================
    
    /**
     * 订单号序列：shopping:order:seq:{yyyyMMdd}
     * 每天重置的订单号序列
     */
    public static final String ORDER_SEQ_KEY = DB_PREFIX + "order:seq:";
    
    /**
     * 订单超时队列：shopping:order:timeout:queue
     * 延迟队列，用于订单超时自动取消
     */
    public static final String ORDER_TIMEOUT_QUEUE = DB_PREFIX + "order:timeout:queue";
    
    /**
     * 订单锁：shopping:lock:order:{orderNo}
     * 用于订单操作时的分布式锁
     */
    public static final String ORDER_LOCK_KEY = DB_PREFIX + "lock:order:";
    
    /**
     * 订单超时时间（毫秒）：30分钟
     */
    public static final long ORDER_TIMEOUT_MS = 30 * 60 * 1000; // 30分钟

    // ==================== 优惠券锁 ====================
    
    /**
     * 优惠券领取锁：shopping:lock:coupon:receive:{couponId}
     * 用于优惠券领取时的分布式锁，防止超发
     */
    public static final String COUPON_RECEIVE_LOCK_KEY = DB_PREFIX + "lock:coupon:receive:";
    
    /**
     * 用户优惠券使用锁：shopping:lock:user:coupon:{userCouponId}
     * 用于优惠券使用时的分布式锁，防止重复使用
     */
    public static final String USER_COUPON_LOCK_KEY = DB_PREFIX + "lock:user:coupon:";

    // ==================== 分布式锁配置 ====================
    
    /**
     * 锁等待时间（秒）
     */
    public static final long LOCK_WAIT_TIME = 5;
    
    /**
     * 锁持有时间（秒）
     */
    public static final long LOCK_LEASE_TIME = 10;

    // ==================== 缓存防护配置 ====================
    
    /**
     * 空对象缓存时间（秒）：5分钟
     * 用于防止缓存穿透
     */
    public static final long EMPTY_CACHE_TTL = 5 * 60; // 5分钟
    
    /**
     * 随机TTL范围（秒）：0-5分钟
     * 用于防止缓存雪崩
     */
    public static final int RANDOM_TTL_RANGE = 5 * 60; // 5分钟
    
    /**
     * 缓存加载锁前缀：shopping:lock:cache:load:
     * 用于防止缓存击穿
     */
    public static final String CACHE_LOAD_LOCK_PREFIX = DB_PREFIX + "lock:cache:load:";

    // ==================== 工具方法 ====================
    
    // 商品相关
    public static String getProductInfoKey(Long productId) {
        return PRODUCT_INFO_KEY + productId;
    }
    
    public static String getProductImagesKey(Long productId) {
        return PRODUCT_IMAGES_KEY + productId;
    }
    
    public static String getProductListKey(Long categoryId, int page, int size) {
        return PRODUCT_LIST_KEY + categoryId + ":" + page + ":" + size;
    }
    
    // 分类相关
    public static String getCategoryInfoKey(Long categoryId) {
        return CATEGORY_INFO_KEY + categoryId;
    }
    
    // 用户相关
    public static String getUserInfoKey(Long userId) {
        return USER_INFO_KEY + userId;
    }
    
    public static String getUserFavoritesKey(Long userId) {
        return USER_FAVORITES_KEY + userId;
    }
    
    // 评论相关
    public static String getReviewListKey(Long productId, int page) {
        return REVIEW_LIST_KEY + productId + ":" + page;
    }
    
    public static String getReviewStatsKey(Long productId) {
        return REVIEW_STATS_KEY + productId;
    }
    
    // 优惠券相关
    public static String getCouponInfoKey(Long couponId) {
        return COUPON_INFO_KEY + couponId;
    }
    
    public static String getUserCouponsKey(Long userId, Integer status) {
        return USER_COUPONS_KEY + userId + ":" + (status != null ? status : "all");
    }
    
    // 库存相关
    public static String getStockKey(Long productId) {
        return STOCK_KEY + productId;
    }
    
    public static String getReservedStockKey(Long productId) {
        return RESERVED_STOCK_KEY + productId;
    }
    
    public static String getStockLockKey(Long productId) {
        return STOCK_LOCK_KEY + productId;
    }
    
    // 订单相关
    public static String getOrderLockKey(String orderNo) {
        return ORDER_LOCK_KEY + orderNo;
    }
    
    // 优惠券锁相关
    public static String getCouponReceiveLockKey(Long couponId) {
        return COUPON_RECEIVE_LOCK_KEY + couponId;
    }
    
    public static String getUserCouponLockKey(Long userCouponId) {
        return USER_COUPON_LOCK_KEY + userCouponId;
    }
    
    /**
     * 构建优惠券锁键（已废弃，使用getUserCouponLockKey）
     * @deprecated 使用 getUserCouponLockKey 替代
     */
    @Deprecated
    public static String getCouponLockKey(Long userCouponId) {
        return USER_COUPON_LOCK_KEY + userCouponId;
    }
    
    // 购物车相关
    public static String getCartKey(Long userId) {
        return CART_KEY + userId;
    }
    
    // 缓存加载锁相关
    public static String getCacheLoadLockKey(String cacheType, Object id) {
        return CACHE_LOAD_LOCK_PREFIX + cacheType + ":" + id;
    }
    
    /**
     * 获取带随机TTL的过期时间（秒）
     * 用于防止缓存雪崩
     * 
     * @param baseTtl 基础TTL（秒）
     * @return 基础TTL + 随机时间（0-5分钟）
     */
    public static int getRandomTtl(long baseTtl) {
        return (int) (baseTtl + (Math.random() * RANDOM_TTL_RANGE));
    }
}
