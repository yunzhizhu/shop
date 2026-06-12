package com.example.shop.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.dto.*;
import com.example.shop.entity.*;
import com.example.shop.enums.OrderStatus;
import com.example.shop.enums.PaymentType;
import com.example.shop.enums.ProductStatus;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.*;
import com.example.shop.service.OrderService;
import com.example.shop.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单服务实现类
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private com.example.shop.service.CouponService couponService;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private com.example.shop.service.StockService stockService;

    @Autowired
    private com.example.shop.helper.FileUploadHelper fileUploadHelper;

    @Autowired
    private com.example.shop.service.OrderNoGeneratorService orderNoGeneratorService;

    @Autowired
    private com.example.shop.service.OrderTimeoutService orderTimeoutService;
    
    @Autowired
    private com.example.shop.service.RedisCacheService redisCacheService;
    
    @Autowired
    private com.example.shop.service.CartRedisService cartRedisService;
    
    /**
     * 库存预占记录（用于回滚）
     */
    private static class StockReservation {
        private Long productId;
        private Integer quantity;
        
        public StockReservation(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
        
        public Long getProductId() {
            return productId;
        }
        
        public Integer getQuantity() {
            return quantity;
        }
    }

    /**
     * 创建订单（从购物车结算）
     * 
     * 金额计算公式：
     * - 商品总金额(totalAmount) = Σ(商品单价 × 购买数量)
     * - 优惠券抵扣(couponAmount) = 根据优惠券类型计算折扣金额
     * - 实付金额(paymentAmount) = 商品总金额 - 优惠券抵扣 + 运费
     * 
     * 库存处理流程：
     * 1. 下单时：预占库存（reserved_stock增加）
     * 2. 支付时：扣减实际库存（stock减少）+ 释放预占（reserved_stock减少）
     * 3. 取消/超时：释放预占库存（reserved_stock减少）
     * 
     * @param request 创建订单请求（包含地址ID、优惠券ID、备注）
     * @return 创建成功的订单对象
     */
    @Override
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();

        // ==================== 步骤1：验证收货地址 ====================
        UserAddress address = userAddressMapper.selectById(request.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(404, "收货地址不存在");
        }

        // ==================== 步骤2：获取购物车选中商品 ====================
        List<Cart> selectedCarts = cartMapper.selectSelectedCartItems(userId);
        if (selectedCarts.isEmpty()) {
            throw new BusinessException(400, "请选择要购买的商品");
        }

        // ==================== 步骤3：验证商品并预占库存 ====================
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;  // 商品总金额
        List<StockReservation> reservations = new ArrayList<>(); // 记录预占信息，用于异常回滚

        for (Cart cart : selectedCarts) {
            // 3.1 验证商品状态
            Product product = productMapper.selectById(cart.getProductId());
            if (product == null) {
                throw new BusinessException(400, "商品不存在: " + cart.getProductId());
            }
            if (!ProductStatus.ON_SHELF.getCode().equals(product.getStatus())) {
                throw new BusinessException(400, "商品已下架: " + product.getName());
            }
            
            // 3.2 检查可用库存（可用库存 = 总库存 - 预占库存）
            Integer availableStock = stockService.getAvailableStock(cart.getProductId());
            if (availableStock < cart.getQuantity()) {
                // 库存不足，回滚之前已预占的库存
                for (StockReservation reservation : reservations) {
                    stockService.releaseReservedStock(reservation.getProductId(), reservation.getQuantity());
                }
                throw new BusinessException(400, 
                    String.format("商品库存不足: %s，当前可用库存：%d", 
                        product.getName(), availableStock));
            }
            
            // 3.3 预占库存（同时更新MySQL和Redis，保证一致性）
            if (!stockService.reserveStock(cart.getProductId(), cart.getQuantity())) {
                // 预占失败，回滚之前已预占的库存
                for (StockReservation reservation : reservations) {
                    stockService.releaseReservedStock(reservation.getProductId(), reservation.getQuantity());
                }
                throw new BusinessException(400, "库存预占失败: " + product.getName() + "，请重试");
            }
            
            // 3.4 记录预占信息（用于后续异常回滚）
            reservations.add(new StockReservation(cart.getProductId(), cart.getQuantity()));
            
            // 3.5 创建订单项，计算单项金额
            // 单项金额 = 商品单价 × 购买数量
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getProductId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setCurrentPrice(product.getPrice());  // 下单时的价格（防止后续价格变动）
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));

            orderItems.add(orderItem);
            // 累加商品总金额
            totalAmount = totalAmount.add(orderItem.getTotalPrice());
        }

        // ==================== 步骤4：计算优惠券抵扣 ====================
        BigDecimal couponAmount = BigDecimal.ZERO;  // 优惠券抵扣金额
        if (request.getUserCouponId() != null) {
            // 验证优惠券是否满足使用门槛
            if (couponService.canUseCoupon(request.getUserCouponId(), totalAmount)) {
                // 计算折扣金额（满减券直接减金额，折扣券按比例计算）
                couponAmount = couponService.calculateDiscount(request.getUserCouponId(), totalAmount);
            } else {
                // 优惠券不可用，回滚已预占的库存
                for (StockReservation reservation : reservations) {
                    stockService.releaseReservedStock(reservation.getProductId(), reservation.getQuantity());
                }
                throw new BusinessException(400, "优惠券不可用");
            }
        }

        // ==================== 步骤5：计算实付金额 ====================
        BigDecimal freightAmount = BigDecimal.ZERO; // 运费（暂时免运费）
        // 实付金额 = 商品总金额 - 优惠券抵扣 + 运费
        BigDecimal paymentAmount = totalAmount.subtract(couponAmount).add(freightAmount);
        
        // 确保实付金额最低为0.01元（避免0元订单）
        if (paymentAmount.compareTo(new BigDecimal("0.01")) < 0) {
            paymentAmount = new BigDecimal("0.01");
        }

        // ==================== 步骤6：创建订单主表 ====================
        Order order = new Order();
        order.setOrderNo(generateOrderNo());           // 生成唯一订单号
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);             // 商品总金额
        order.setPaymentAmount(paymentAmount);         // 实付金额
        order.setFreightAmount(freightAmount);         // 运费
        order.setCouponAmount(couponAmount);           // 优惠券抵扣金额
        order.setUserCouponId(request.getUserCouponId());
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());  // 待支付状态
        order.setAddressId(request.getAddressId());
        order.setRemark(request.getRemark());

        int result = orderMapper.insert(order);
        if (result <= 0) {
            // 订单创建失败，回滚已预占的库存
            for (StockReservation reservation : reservations) {
                stockService.releaseReservedStock(reservation.getProductId(), reservation.getQuantity());
            }
            throw new BusinessException("订单创建失败");
        }

        // ==================== 步骤7：创建订单项（批量插入） ====================
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getOrderId());
            orderItem.setOrderNo(order.getOrderNo());
        }
        
        try {
            orderItemMapper.insertBatch(orderItems);
        } catch (Exception e) {
            // 订单项创建失败，回滚已预占的库存
            for (StockReservation reservation : reservations) {
                stockService.releaseReservedStock(reservation.getProductId(), reservation.getQuantity());
            }
            log.error("创建订单项失败: orderNo={}", order.getOrderNo(), e);
            throw new BusinessException("创建订单项失败");
        }

        // ==================== 步骤8：使用优惠券 ====================
        if (request.getUserCouponId() != null) {
            try {
                couponService.useCoupon(request.getUserCouponId(), order.getOrderId());
            } catch (Exception e) {
                // 优惠券使用失败，回滚已预占的库存
                for (StockReservation reservation : reservations) {
                    stockService.releaseReservedStock(reservation.getProductId(), reservation.getQuantity());
                }
                log.error("使用优惠券失败: orderNo={}, userCouponId={}", order.getOrderNo(), request.getUserCouponId(), e);
                throw new BusinessException("使用优惠券失败");
            }
        }

        // ==================== 步骤9：清空购物车选中项 ====================
        cartMapper.deleteSelectedItems(userId);
        
        // 同时清空Redis缓存（保证MySQL和Redis数据一致性）
        try {
            cartRedisService.clearCart(userId);
            log.info("清空购物车Redis缓存成功: userId={}", userId);
        } catch (Exception e) {
            log.error("清空购物车Redis缓存失败: userId={}", userId, e);
            // Redis清空失败不影响订单创建，只记录日志
        }

        // ==================== 步骤10：添加订单到超时队列 ====================
        // 30分钟后未支付自动取消，释放预占库存
        orderTimeoutService.addOrderToTimeoutQueue(order.getOrderNo());

        log.info("订单创建成功: orderNo={}, userId={}, totalAmount={}, couponAmount={}",
                order.getOrderNo(), userId, totalAmount, couponAmount);
        return order;
    }

    /**
     * 立即购买（直接购买单个商品，不经过购物车）
     * 
     * 金额计算公式：
     * - 商品总金额(totalAmount) = 商品单价 × 购买数量
     * - 优惠券抵扣(couponAmount) = 根据优惠券类型计算折扣金额
     * - 实付金额(paymentAmount) = 商品总金额 - 优惠券抵扣 + 运费
     * 
     * 与createOrder的区别：
     * 1. 不需要从购物车获取商品，直接指定商品ID和数量
     * 2. 不需要清空购物车
     * 3. 只涉及单个商品，逻辑更简单
     * 
     * @param request 立即购买请求（包含商品ID、数量、地址ID、优惠券ID、备注）
     * @return 创建成功的订单对象
     */
    @Override
    @Transactional
    public Order buyNow(BuyNowRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();

        // ==================== 步骤1：验证商品状态 ====================
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!ProductStatus.ON_SHELF.getCode().equals(product.getStatus())) {
            throw new BusinessException(400, "商品已下架");
        }

        // ==================== 步骤2：验证收货地址 ====================
        UserAddress address = userAddressMapper.selectById(request.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(404, "收货地址不存在");
        }

        // ==================== 步骤3：预占库存 ====================
        // 检查可用库存（可用库存 = 总库存 - 预占库存）
        Integer availableStock = stockService.getAvailableStock(request.getProductId());
        if (availableStock < request.getQuantity()) {
            throw new BusinessException(400, 
                String.format("商品库存不足: %s，当前可用库存：%d", 
                    product.getName(), availableStock));
        }
        
        // 预占库存（同时更新MySQL和Redis，保证一致性）
        if (!stockService.reserveStock(request.getProductId(), request.getQuantity())) {
            throw new BusinessException(400, "库存预占失败，请重试");
        }
        
        log.info("立即购买预占库存成功: productId={}, quantity={}", request.getProductId(), request.getQuantity());

        // ==================== 步骤4：计算商品总金额 ====================
        // 商品总金额 = 商品单价 × 购买数量
        BigDecimal unitPrice = product.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        // ==================== 步骤5：计算优惠券抵扣 ====================
        BigDecimal couponAmount = BigDecimal.ZERO;  // 优惠券抵扣金额
        if (request.getUserCouponId() != null) {
            // 验证优惠券是否满足使用门槛
            if (couponService.canUseCoupon(request.getUserCouponId(), totalAmount)) {
                // 计算折扣金额（满减券直接减金额，折扣券按比例计算）
                couponAmount = couponService.calculateDiscount(request.getUserCouponId(), totalAmount);
            } else {
                // 优惠券不可用，回滚预占库存
                stockService.releaseReservedStock(request.getProductId(), request.getQuantity());
                throw new BusinessException(400, "优惠券不可用");
            }
        }

        // ==================== 步骤6：计算实付金额 ====================
        BigDecimal freightAmount = BigDecimal.ZERO; // 运费（暂时免运费）
        // 实付金额 = 商品总金额 - 优惠券抵扣 + 运费
        BigDecimal paymentAmount = totalAmount.subtract(couponAmount).add(freightAmount);
        
        // 确保实付金额最低为0.01元（避免0元订单）
        if (paymentAmount.compareTo(new BigDecimal("0.01")) < 0) {
            paymentAmount = new BigDecimal("0.01");
        }

        // ==================== 步骤7：创建订单主表 ====================
        Order order = new Order();
        order.setOrderNo(generateOrderNo());           // 生成唯一订单号
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);             // 商品总金额
        order.setPaymentAmount(paymentAmount);         // 实付金额
        order.setFreightAmount(freightAmount);         // 运费
        order.setCouponAmount(couponAmount);           // 优惠券抵扣金额
        order.setUserCouponId(request.getUserCouponId());
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());  // 待支付状态
        order.setAddressId(request.getAddressId());
        order.setRemark(request.getRemark());

        int result = orderMapper.insert(order);
        if (result <= 0) {
            // 订单创建失败，回滚预占库存
            stockService.releaseReservedStock(request.getProductId(), request.getQuantity());
            throw new BusinessException("订单创建失败");
        }

        // ==================== 步骤8：创建订单项 ====================
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getOrderId());
        orderItem.setOrderNo(order.getOrderNo());
        orderItem.setProductId(product.getProductId());
        orderItem.setProductName(product.getName());
        orderItem.setProductImage(product.getMainImage());
        orderItem.setCurrentPrice(unitPrice);          // 下单时的价格
        orderItem.setQuantity(request.getQuantity());
        orderItem.setTotalPrice(totalAmount);          // 单项金额 = 单价 × 数量

        try {
            orderItemMapper.insert(orderItem);
        } catch (Exception e) {
            // 订单项创建失败，回滚预占库存
            stockService.releaseReservedStock(request.getProductId(), request.getQuantity());
            log.error("创建订单项失败: orderNo={}", order.getOrderNo(), e);
            throw new BusinessException("创建订单项失败");
        }

        // ==================== 步骤9：使用优惠券 ====================
        if (request.getUserCouponId() != null) {
            try {
                couponService.useCoupon(request.getUserCouponId(), order.getOrderId());
            } catch (Exception e) {
                // 优惠券使用失败，回滚预占库存
                stockService.releaseReservedStock(request.getProductId(), request.getQuantity());
                log.error("使用优惠券失败: orderNo={}, userCouponId={}", order.getOrderNo(), request.getUserCouponId(), e);
                throw new BusinessException("使用优惠券失败");
            }
        }

        // ==================== 步骤10：添加订单到超时队列 ====================
        // 30分钟后未支付自动取消，释放预占库存
        orderTimeoutService.addOrderToTimeoutQueue(order.getOrderNo());

        log.info("立即购买订单创建成功: orderNo={}, userId={}, productId={}, quantity={}, totalAmount={}, couponAmount={}",
                order.getOrderNo(), userId, request.getProductId(), request.getQuantity(), totalAmount, couponAmount);
        return order;
    }

    @Override
    public OrderDetailResponse getOrderDetail(String orderNo) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 检查订单权限
        if (!checkOrderPermission(orderNo, userId)) {
            throw new BusinessException(403, "无权限查看此订单");
        }

        // 获取订单详情
        Order order = orderMapper.selectOrderDetailByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 获取用户信息
        User user = userMapper.selectById(order.getUserId());

        // 获取订单商品
        List<OrderItem> orderItems = orderItemMapper.selectByOrderNo(orderNo);

        // 获取地址信息
        UserAddress address = userAddressMapper.selectById(order.getAddressId());

        // 构建响应
        OrderDetailResponse response = new OrderDetailResponse();
        BeanUtils.copyProperties(order, response);
        
        // 设置用户信息
        if (user != null) {
            response.setUserId(user.getUserId());
            response.setUsername(user.getUsername());
            response.setUserPhone(user.getPhone());
        }
        
        // 设置状态描述
        OrderStatus orderStatus = OrderStatus.getByCode(order.getStatus());
        response.setStatusDesc(orderStatus != null ? orderStatus.getDescription() : "未知状态");
        
        // 设置支付方式描述
        if (order.getPaymentType() != null) {
            PaymentType paymentType = PaymentType.getByCode(order.getPaymentType());
            response.setPaymentTypeDesc(paymentType != null ? paymentType.getDescription() : "未知支付方式");
        }

        // 设置地址信息
        if (address != null) {
            OrderDetailResponse.AddressInfo addressInfo = new OrderDetailResponse.AddressInfo();
            addressInfo.setReceiverName(address.getReceiverName());
            addressInfo.setReceiverPhone(address.getReceiverPhone());
            addressInfo.setFullAddress(address.getProvince() + address.getCity() + 
                                     address.getDistrict() + address.getDetailAddress());
            response.setAddress(addressInfo);
        }

        // 设置订单商品
        List<OrderDetailResponse.OrderItemInfo> itemInfos = orderItems.stream()
                .map(item -> {
                    OrderDetailResponse.OrderItemInfo itemInfo = new OrderDetailResponse.OrderItemInfo();
                    itemInfo.setProductId(item.getProductId());
                    itemInfo.setName(item.getProductName());
                    
                    // 如果订单项的图片为空，尝试从商品图片表获取
                    String imageUrl = item.getProductImage();
                    if (imageUrl == null || imageUrl.isEmpty()) {
                        List<com.example.shop.entity.ProductImage> productImages = 
                            productImageMapper.selectByProductId(item.getProductId());
                        if (!productImages.isEmpty()) {
                            // 优先使用主图，否则使用第一张图片
                            imageUrl = productImages.stream()
                                .filter(img -> img.getIsMain() == 1)
                                .findFirst()
                                .map(com.example.shop.entity.ProductImage::getImageUrl)
                                .orElse(productImages.get(0).getImageUrl());
                        }
                    }
                    
                    // 转换为完整URL
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        imageUrl = fileUploadHelper.toFullUrl(imageUrl);
                    }
                    itemInfo.setImage(imageUrl);
                    
                    itemInfo.setPrice(item.getCurrentPrice());
                    itemInfo.setQuantity(item.getQuantity());
                    itemInfo.setTotalPrice(item.getTotalPrice());
                    return itemInfo;
                })
                .toList();
        response.setItems(itemInfos);

        return response;
    }

    @Override
    public IPage<Order> getUserOrderPage(int page, int size, Integer status) {
        Long userId = SecurityUtil.getCurrentUserId();
        Page<Order> pageParam = new Page<>(page, size);
        return orderMapper.selectUserOrderPage(pageParam, userId, status);
    }

    @Override
    public IPage<OrderListResponse> getUserOrderPageWithItems(int page, int size, Integer status) {
        Long userId = SecurityUtil.getCurrentUserId();
        Page<OrderListResponse> pageParam = new Page<>(page, size);
        IPage<OrderListResponse> orderPage = orderMapper.selectUserOrderPageWithItems(pageParam, userId, status);
        
        // 转换商品图片为完整URL
        orderPage.getRecords().forEach(order -> {
            if (order.getItems() != null) {
                order.getItems().forEach(item -> {
                    if (item.getProductImage() != null && !item.getProductImage().isEmpty()) {
                        item.setProductImage(fileUploadHelper.toFullUrl(item.getProductImage()));
                    }
                });
            }
        });
        
        return orderPage;
    }

    @Override
    public IPage<com.example.shop.dto.AdminOrderListResponse> getOrderPageForAdmin(int page, int size, Integer status, 
                                           LocalDateTime startTime, LocalDateTime endTime, String keyword) {
        Page<com.example.shop.dto.AdminOrderListResponse> pageParam = new Page<>(page, size);
        return orderMapper.selectOrderPageForAdmin(pageParam, status, startTime, endTime, keyword);
    }

    @Override
    @Transactional
    public void payOrder(OrderPayRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 检查订单权限
        if (!checkOrderPermission(request.getOrderNo(), userId)) {
            throw new BusinessException(403, "无权限操作此订单");
        }

        // 获取订单
        Order order = orderMapper.selectByOrderNo(request.getOrderNo());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 检查订单状态
        OrderStatus orderStatus = OrderStatus.getByCode(order.getStatus());
        if (orderStatus == null || !orderStatus.canPay()) {
            throw new BusinessException(400, "订单状态不允许支付");
        }

        // 验证订单中的商品是否仍然上架（防止支付已下架商品）
        List<OrderItem> orderItems = orderItemMapper.selectByOrderNo(request.getOrderNo());
        for (OrderItem item : orderItems) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                throw new BusinessException(400, "商品不存在: " + item.getProductName());
            }
            if (!ProductStatus.ON_SHELF.getCode().equals(product.getStatus())) {
                throw new BusinessException(400, "商品已下架，无法支付: " + item.getProductName());
            }
        }

        // 验证支付方式
        PaymentType paymentType = PaymentType.getByCode(request.getPaymentType());
        if (paymentType == null) {
            throw new BusinessException(400, "无效的支付方式");
        }

        // 处理不同的支付方式
        if (PaymentType.ALIPAY.equals(paymentType)) {
            // 待实现：集成支付宝支付SDK
            // 当前为模拟支付，直接标记为已支付
            // 生产环境需要：
            // 1. 调用支付宝统一下单接口
            // 2. 返回支付参数给前端
            // 3. 前端调起支付宝支付
            // 4. 接收支付宝异步回调通知
            // 5. 验证签名后更新订单状态
            log.warn("支付宝支付为模拟实现，订单将直接标记为已支付: orderNo={}", request.getOrderNo());
            
        } else if (PaymentType.WECHAT.equals(paymentType)) {
            // 待实现：集成微信支付SDK
            // 当前为模拟支付，直接标记为已支付
            // 生产环境需要：
            // 1. 调用微信统一下单接口
            // 2. 返回支付参数给前端
            // 3. 前端调起微信支付
            // 4. 接收微信异步回调通知
            // 5. 验证签名后更新订单状态
            log.warn("微信支付为模拟实现，订单将直接标记为已支付: orderNo={}", request.getOrderNo());
            
        } else if (PaymentType.BALANCE.equals(paymentType)) {
            // 余额支付：真实扣款
            User user = userMapper.selectById(userId);
            if (user.getBalance().compareTo(order.getPaymentAmount()) < 0) {
                throw new BusinessException(400, "余额不足");
            }
            
            // 扣减余额
            user.setBalance(user.getBalance().subtract(order.getPaymentAmount()));
            userMapper.updateById(user);
            
            // 清除用户缓存（重要：确保前端能获取到最新余额）
            String userCacheKey = com.example.shop.constants.RedisConstants.getUserInfoKey(userId);
            redisCacheService.delete(userCacheKey);
            
            log.info("余额支付成功，扣减余额: userId={}, amount={}, 已清除用户缓存", 
                    userId, order.getPaymentAmount());
        }

        // 更新订单支付信息
        int result = orderMapper.updatePaymentInfo(
                request.getOrderNo(),
                request.getPaymentType(),
                LocalDateTime.now(),
                OrderStatus.PAID.getCode(),
                order.getVersion()
        );

        if (result <= 0) {
            throw new BusinessException("支付失败");
        }

        // 支付成功后：将预占库存转为实际扣减，并更新商品销量
        for (OrderItem item : orderItems) {
            try {
                // 1. 扣减实际库存（同时更新MySQL和Redis）
                if (!stockService.deductStock(item.getProductId(), item.getQuantity())) {
                    log.error("扣减库存失败: productId={}, quantity={}", item.getProductId(), item.getQuantity());
                    throw new BusinessException("库存扣减失败");
                }
                
                // 2. 释放预占库存（同时更新MySQL和Redis）
                if (!stockService.releaseReservedStock(item.getProductId(), item.getQuantity())) {
                    log.error("释放预占库存失败: productId={}, quantity={}", item.getProductId(), item.getQuantity());
                    // 预占库存释放失败不影响支付流程，只记录日志
                }
                
                // 3. 更新商品销量
                productMapper.updateSales(item.getProductId(), item.getQuantity());
                log.info("支付成功处理库存: productId={}, quantity={}, 扣减库存+释放预占+更新销量", 
                         item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("支付后库存处理失败: productId={}, quantity={}, error={}", 
                         item.getProductId(), item.getQuantity(), e.getMessage());
                throw new BusinessException("库存处理失败: " + e.getMessage());
            }
        }

        // 从超时队列中移除订单（已支付，无需超时取消）
        orderTimeoutService.removeOrderFromTimeoutQueue(request.getOrderNo());

        log.info("订单支付成功: orderNo={}, paymentType={}", request.getOrderNo(), request.getPaymentType());
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNo) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 检查订单权限
        if (!checkOrderPermission(orderNo, userId)) {
            throw new BusinessException(403, "无权限操作此订单");
        }

        // 获取订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 检查订单状态
        OrderStatus orderStatus = OrderStatus.getByCode(order.getStatus());
        if (orderStatus == null || !orderStatus.canCancel()) {
            throw new BusinessException(400, "订单状态不允许取消");
        }

        // 释放预占库存（同时更新MySQL和Redis）
        List<OrderItem> orderItems = orderItemMapper.selectByOrderNo(orderNo);
        for (OrderItem item : orderItems) {
            if (stockService.releaseReservedStock(item.getProductId(), item.getQuantity())) {
                log.info("释放预占库存成功: productId={}, quantity={}", item.getProductId(), item.getQuantity());
            } else {
                log.error("释放预占库存失败: productId={}, quantity={}", item.getProductId(), item.getQuantity());
            }
        }

        // 退还优惠券
        if (order.getUserCouponId() != null) {
            couponService.returnCoupon(order.getUserCouponId());
        }

        // 更新订单状态
        int result = orderMapper.updateOrderStatus(orderNo, OrderStatus.CANCELLED.getCode(), order.getVersion());
        if (result <= 0) {
            throw new BusinessException("取消订单失败");
        }

        // 从超时队列中移除订单（已手动取消，无需超时取消）
        orderTimeoutService.removeOrderFromTimeoutQueue(orderNo);

        log.info("订单取消成功: orderNo={}", orderNo);
    }

    @Override
    @Transactional
    public void deliverOrder(String orderNo, String deliveryCompany, String trackingNo) {
        // 仅管理员可以发货
        if (!SecurityUtil.isAdmin()) {
            throw new BusinessException(403, "权限不足，仅管理员可以发货");
        }
        
        // 获取订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 检查订单状态
        OrderStatus orderStatus = OrderStatus.getByCode(order.getStatus());
        if (orderStatus == null || !orderStatus.canDeliver()) {
            throw new BusinessException(400, "订单状态不允许发货");
        }

        // 更新发货信息
        int result = orderMapper.updateDeliveryInfo(
                orderNo,
                LocalDateTime.now(),
                OrderStatus.SHIPPED.getCode(),
                order.getVersion()
        );

        if (result <= 0) {
            throw new BusinessException("发货失败");
        }

        log.info("订单发货成功: orderNo={}, deliveryCompany={}, trackingNo={}", 
                orderNo, deliveryCompany, trackingNo);
    }

    @Override
    @Transactional
    public void receiveOrder(String orderNo) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 检查订单权限
        if (!checkOrderPermission(orderNo, userId)) {
            throw new BusinessException(403, "无权限操作此订单");
        }

        // 获取订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 检查订单状态
        OrderStatus orderStatus = OrderStatus.getByCode(order.getStatus());
        if (orderStatus == null || !orderStatus.canReceive()) {
            throw new BusinessException(400, "订单状态不允许确认收货");
        }

        // 更新订单状态
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setReceiveTime(LocalDateTime.now());
        
        int result = orderMapper.updateById(order);
        if (result <= 0) {
            throw new BusinessException("确认收货失败");
        }

        log.info("订单确认收货成功: orderNo={}", orderNo);
    }

    @Override
    public String generateOrderNo() {
        // 使用Redis INCR生成分布式唯一订单号
        return orderNoGeneratorService.generateOrderNo();
    }

    @Override
    public boolean checkOrderPermission(String orderNo, Long userId) {
        // 管理员可以访问所有订单
        if (SecurityUtil.isAdmin()) {
            return true;
        }
        
        Order order = orderMapper.selectByOrderNo(orderNo);
        return order != null && order.getUserId().equals(userId);
    }

    @Override
    public int cancelExpiredOrders() {
        // 查询创建时间超过30分钟且状态为待支付的订单
        List<Order> expiredOrders = orderMapper.selectExpiredOrders(30);
        
        if (expiredOrders.isEmpty()) {
            return 0;
        }

        int cancelledCount = 0;
        for (Order order : expiredOrders) {
            try {
                // 每个订单独立处理，使用独立事务
                cancelSingleExpiredOrder(order);
                cancelledCount++;
            } catch (Exception e) {
                log.error("订单超时取消处理失败: orderNo={}, error={}", 
                         order.getOrderNo(), e.getMessage(), e);
            }
        }

        return cancelledCount;
    }

    /**
     * 取消单个超时订单（独立事务）
     */
    @Transactional
    private void cancelSingleExpiredOrder(Order order) {
        // 1. 更新订单状态为已取消
        int result = orderMapper.updateOrderStatus(
            order.getOrderNo(), 
            OrderStatus.CANCELLED.getCode(), 
            order.getVersion()
        );
        
        if (result <= 0) {
            log.warn("订单状态更新失败（可能已被其他操作修改）: orderNo={}", order.getOrderNo());
            return;
        }

        // 2. 释放预占库存（同时更新MySQL和Redis）
        List<OrderItem> orderItems = orderItemMapper.selectByOrderNo(order.getOrderNo());
        for (OrderItem item : orderItems) {
            if (stockService.releaseReservedStock(item.getProductId(), item.getQuantity())) {
                log.info("释放预占库存成功: orderNo={}, productId={}, quantity={}", 
                        order.getOrderNo(), item.getProductId(), item.getQuantity());
            } else {
                log.error("释放预占库存失败: orderNo={}, productId={}, quantity={}", 
                         order.getOrderNo(), item.getProductId(), item.getQuantity());
            }
        }

        // 3. 退还优惠券（如果失败不影响订单取消）
        if (order.getUserCouponId() != null) {
            try {
                // 使用内部方法，不需要用户上下文
                ((CouponServiceImpl) couponService).returnCouponInternal(order.getUserCouponId());
                log.info("退还优惠券成功: orderNo={}, userCouponId={}", 
                        order.getOrderNo(), order.getUserCouponId());
            } catch (Exception e) {
                // 优惠券退还失败不影响订单取消，只记录日志
                log.error("退还优惠券失败（不影响订单取消）: orderNo={}, userCouponId={}, error={}", 
                         order.getOrderNo(), order.getUserCouponId(), e.getMessage());
            }
        }

        log.info("订单超时自动取消成功: orderNo={}, userId={}, totalAmount={}", 
                order.getOrderNo(), order.getUserId(), order.getTotalAmount());
    }
}
