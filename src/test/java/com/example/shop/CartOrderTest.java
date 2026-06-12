package com.example.shop;

import com.example.shop.mapper.CartMapper;
import com.example.shop.mapper.OrderMapper;
import com.example.shop.mapper.OrderItemMapper;
import com.example.shop.service.CartService;
import com.example.shop.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CartOrderTest {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Test
    public void testMapperLoading() {
        // 简单测试Mapper是否能正常加载
        System.out.println("CartMapper loaded: " + (cartMapper != null));
        System.out.println("OrderMapper loaded: " + (orderMapper != null));
        System.out.println("OrderItemMapper loaded: " + (orderItemMapper != null));
        System.out.println("CartService loaded: " + (cartService != null));
        System.out.println("OrderService loaded: " + (orderService != null));
    }
}
