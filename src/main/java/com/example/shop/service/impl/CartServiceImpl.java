package com.example.shop.service.impl;

import com.example.shop.dto.*;
import com.example.shop.entity.Cart;
import com.example.shop.entity.Product;
import com.example.shop.enums.ProductStatus;
import com.example.shop.exception.BusinessException;
import com.example.shop.mapper.CartMapper;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.service.CartRedisService;
import com.example.shop.service.CartService;
import com.example.shop.service.StockService;
import com.example.shop.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车服务实现类
 * v2.0 集成Redis缓存，提升性能
 */
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StockService stockService;

    @Autowired
    private CartRedisService cartRedisService;

    @Autowired
    private com.example.shop.helper.FileUploadHelper fileUploadHelper;

    @Override
    @Transactional
    public Long addToCart(CartAddRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 检查商品是否存在且上架
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!ProductStatus.ON_SHELF.getCode().equals(product.getStatus())) {
            throw new BusinessException(400, "商品已下架");
        }

        // v3.0 订单预占模式：购物车不限制数量，不预占库存
        // 库存校验在提交订单时进行

        // 检查购物车中是否已存在该商品
        Cart existingCart = cartMapper.selectByUserIdAndProductId(userId, request.getProductId());
        
        Cart cart;
        if (existingCart != null) {
            // 更新数量（不预占库存）
            int newQuantity = existingCart.getQuantity() + request.getQuantity();
            existingCart.setQuantity(newQuantity);
            existingCart.setIsSelected(1); // 重新选中
            
            // 1. 先更新MySQL（事务保护）
            int result = cartMapper.updateById(existingCart);
            if (result <= 0) {
                throw new BusinessException("更新购物车失败");
            }
            
            cart = existingCart;
            log.info("更新购物车成功: userId={}, productId={}, quantity={}", 
                    userId, request.getProductId(), newQuantity);
        } else {
            // 新增购物车项（不预占库存）
            cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(request.getProductId());
            cart.setQuantity(request.getQuantity());
            cart.setIsSelected(1);
            
            // 1. 先写入MySQL（事务保护）
            int result = cartMapper.insert(cart);
            if (result <= 0) {
                throw new BusinessException("添加购物车失败");
            }
            
            log.info("添加购物车成功: userId={}, productId={}, quantity={}", 
                    userId, request.getProductId(), request.getQuantity());
        }
        
        // 2. 再更新Redis（失败不影响业务）
        try {
            cartRedisService.saveCartItem(userId, cart);
        } catch (Exception e) {
            log.error("Redis写入失败，但MySQL已成功: userId={}, productId={}", 
                    userId, request.getProductId(), e);
            // 不抛出异常，保证业务成功
        }
        
        return cart.getCartId();
    }

    @Override
    @Transactional
    public void updateCartItem(CartUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 检查购物车项是否存在且属于当前用户
        Cart cart = cartMapper.selectById(request.getCartId());
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(404, "购物车项不存在");
        }

        // 验证版本号是否匹配
        if (!cart.getVersion().equals(request.getVersion())) {
            throw new BusinessException(409, "数据已被修改，请刷新后重试");
        }

        // 检查商品库存
        Product product = productMapper.selectById(cart.getProductId());
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }

        int oldQuantity = cart.getQuantity();
        int newQuantity = request.getQuantity();

        // 验证新数量的合理性
        if (newQuantity < 0) {
            throw new BusinessException(400, "商品数量不能为负数");
        }
        
        // 如果数量为0，删除购物车项
        if (newQuantity == 0) {
            // 1. 先删除MySQL
            int result = cartMapper.deleteById(request.getCartId());
            if (result <= 0) {
                throw new BusinessException("删除购物车项失败");
            }
            
            log.info("删除购物车项: cartId={}, productId={}, quantity={}", 
                    request.getCartId(), cart.getProductId(), oldQuantity);
            
            // 2. 再删除Redis
            try {
                cartRedisService.deleteCartItem(userId, cart.getProductId());
            } catch (Exception e) {
                log.error("Redis删除失败: userId={}, productId={}", 
                        userId, cart.getProductId(), e);
            }
            return;
        }
        
        // v3.0 订单预占模式：购物车不限制数量，不预占库存
        // 库存校验在提交订单时进行

        // 更新数量（不预占库存）
        cart.setQuantity(newQuantity);
        
        // 1. 先更新MySQL
        int result = cartMapper.updateById(cart);
        if (result <= 0) {
            throw new BusinessException(409, "更新失败，数据可能已被其他操作修改，请刷新后重试");
        }

        log.info("更新购物车成功: cartId={}, oldQuantity={}, newQuantity={}", 
                request.getCartId(), oldQuantity, newQuantity);
        
        // 2. 再更新Redis
        try {
            cartRedisService.saveCartItem(userId, cart);
        } catch (Exception e) {
            log.error("Redis更新失败: userId={}, productId={}", 
                    userId, cart.getProductId(), e);
        }
    }

    @Override
    @Transactional
    public void updateSelectedStatus(CartSelectRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 1. 先更新MySQL
        int result = cartMapper.updateSelectedStatus(request.getCartIds(), userId, request.getSelected());
        if (result <= 0) {
            throw new BusinessException("更新选中状态失败");
        }

        log.info("更新购物车选中状态成功: cartIds={}, selected={}", 
                request.getCartIds(), request.getSelected());
        
        // 2. 再更新Redis（重新加载整个购物车）
        try {
            cartRedisService.loadFromDatabase(userId);
        } catch (Exception e) {
            log.error("Redis更新失败: userId={}", userId, e);
        }
    }

    @Override
    public List<CartItemResponse> getCartItems() {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 1. 优先从Redis读取
        try {
            if (cartRedisService.existsCart(userId)) {
                List<Cart> carts = cartRedisService.getUserCart(userId);
                if (carts != null) {
                    // 刷新过期时间
                    cartRedisService.refreshExpire(userId);
                    
                    // 转换为Response并补充商品信息
                    List<CartItemResponse> responses = convertToResponse(carts);
                    log.debug("从Redis获取购物车成功: userId={}, count={}", userId, responses.size());
                    return responses;
                }
            }
        } catch (Exception e) {
            log.error("Redis读取失败，降级到MySQL: userId={}", userId, e);
        }
        
        // 2. Redis未命中或失败，从MySQL读取
        List<CartItemResponse> cartItems = cartMapper.selectCartItemsByUserId(userId);
        
        // 转换商品主图为完整URL
        cartItems.forEach(item -> {
            if (item.getMainImage() != null && !item.getMainImage().isEmpty()) {
                item.setMainImage(fileUploadHelper.toFullUrl(item.getMainImage()));
            }
        });
        
        // 3. 异步写入Redis（懒加载）
        try {
            cartRedisService.loadFromDatabase(userId);
        } catch (Exception e) {
            log.error("Redis缓存失败: userId={}", userId, e);
        }
        
        log.debug("从MySQL获取购物车成功: userId={}, count={}", userId, cartItems.size());
        return cartItems;
    }
    
    /**
     * 将Cart列表转换为CartItemResponse列表
     */
    private List<CartItemResponse> convertToResponse(List<Cart> carts) {
        return carts.stream()
                .map(cart -> {
                    Product product = productMapper.selectById(cart.getProductId());
                    CartItemResponse response = new CartItemResponse();
                    response.setCartId(cart.getCartId());
                    response.setProductId(cart.getProductId());
                    response.setQuantity(cart.getQuantity());
                    response.setIsSelected(cart.getIsSelected());
                    response.setVersion(cart.getVersion());
                    
                    if (product != null) {
                        response.setName(product.getName());
                        // 转换主图为完整URL
                        String mainImage = product.getMainImage();
                        if (mainImage != null && !mainImage.isEmpty()) {
                            mainImage = fileUploadHelper.toFullUrl(mainImage);
                        }
                        response.setMainImage(mainImage);
                        response.setPrice(product.getPrice());
                        response.setStock(product.getStock());
                        response.setStatus(product.getStatus());
                    }
                    
                    return response;
                })
                .toList();
    }

    @Override
    @Transactional
    public void deleteCartItems(List<Long> cartIds) {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 先查询要删除的商品ID列表（用于删除Redis）
        List<Long> productIds = new ArrayList<>();
        for (Long cartId : cartIds) {
            Cart cart = cartMapper.selectById(cartId);
            if (cart != null && cart.getUserId().equals(userId)) {
                productIds.add(cart.getProductId());
            }
        }
        
        // 1. 先删除MySQL
        int result = cartMapper.deleteByUserIdAndCartIds(userId, cartIds);
        if (result <= 0) {
            throw new BusinessException("删除购物车项失败");
        }

        log.info("删除购物车项成功: userId={}, cartIds={}, count={}", userId, cartIds, result);
        
        // 2. 再删除Redis
        try {
            if (!productIds.isEmpty()) {
                cartRedisService.deleteCartItems(userId, productIds);
            }
        } catch (Exception e) {
            log.error("Redis删除失败: userId={}", userId, e);
        }
    }

    @Override
    @Transactional
    public void clearCart() {
        Long userId = SecurityUtil.getCurrentUserId();
        
        // 1. 先删除MySQL中选中的商品
        int result = cartMapper.deleteSelectedItems(userId);
        
        log.info("清空购物车成功: userId={}, deletedCount={}", userId, result);
        
        // 2. 清空Redis后重新从MySQL加载（保留未选中的商品）
        try {
            cartRedisService.clearCart(userId);
            cartRedisService.loadFromDatabase(userId);
        } catch (Exception e) {
            log.error("Redis同步失败: userId={}", userId, e);
        }
    }

    @Override
    public Integer getCartItemCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        // 未登录用户返回0
        if (userId == null) {
            return 0;
        }
        return cartMapper.countCartItems(userId);
    }

    @Override
    public List<CartItemResponse> getSelectedCartItems() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Cart> selectedCarts = cartMapper.selectSelectedCartItems(userId);
        
        // 转换为CartItemResponse
        return selectedCarts.stream()
                .map(cart -> {
                    Product product = productMapper.selectById(cart.getProductId());
                    CartItemResponse response = new CartItemResponse();
                    response.setCartId(cart.getCartId());
                    response.setProductId(cart.getProductId());
                    response.setQuantity(cart.getQuantity());
                    // 强制设置为选中状态，因为这个接口专门返回选中的商品
                    response.setIsSelected(1);
                    response.setVersion(cart.getVersion());
                    
                    if (product != null) {
                        response.setName(product.getName());
                        // 转换主图为完整URL
                        String mainImage = product.getMainImage();
                        if (mainImage != null && !mainImage.isEmpty()) {
                            mainImage = fileUploadHelper.toFullUrl(mainImage);
                        }
                        response.setMainImage(mainImage);
                        response.setPrice(product.getPrice());
                        response.setStock(product.getStock());
                        response.setStatus(product.getStatus());
                    }
                    
                    return response;
                })
                .toList();
    }

    @Override
    public List<CartValidationResult> validateCartItems() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<CartItemResponse> cartItems = getCartItems();
        return validateCartItemsList(cartItems);
    }

    @Override
    public List<CartValidationResult> validateSelectedCartItems() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<CartItemResponse> selectedItems = getSelectedCartItems();
        return validateCartItemsList(selectedItems);
    }

    /**
     * 校验购物车商品列表
     * 购物车不预占库存，直接检查可用库存
     */
    private List<CartValidationResult> validateCartItemsList(List<CartItemResponse> cartItems) {
        List<CartValidationResult> results = new ArrayList<>();
        
        for (CartItemResponse item : cartItems) {
            CartValidationResult result = new CartValidationResult();
            result.setCartId(item.getCartId());
            result.setProductId(item.getProductId());
            result.setProductName(item.getName());
            result.setRequestQuantity(item.getQuantity());
            
            // 检查商品状态
            if (item.getStatus() != 1) {
                result.setIsValid(false);
                result.setErrorType("PRODUCT_OFFLINE");
                result.setSuggestion("商品已下架，建议移除");
                result.setSuggestedQuantity(0);
                results.add(result);
                continue;
            }
            
            // 购物车不预占库存，检查可用库存
            Integer availableStock = stockService.getAvailableStock(item.getProductId());
            result.setAvailableStock(availableStock);
            
            if (availableStock >= item.getQuantity()) {
                result.setIsValid(true);
                result.setSuggestedQuantity(item.getQuantity());
            } else if (availableStock > 0) {
                result.setIsValid(false);
                result.setErrorType("INSUFFICIENT_STOCK");
                result.setSuggestedQuantity(availableStock);
                result.setSuggestion(String.format("库存不足，当前可用%d件，建议修改数量", availableStock));
            } else {
                result.setIsValid(false);
                result.setErrorType("SOLD_OUT");
                result.setSuggestedQuantity(0);
                result.setSuggestion("商品已售罄，建议移除");
            }
            
            results.add(result);
        }
        
        return results;
    }
}
