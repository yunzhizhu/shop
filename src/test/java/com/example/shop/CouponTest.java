package com.example.shop;

import com.example.shop.mapper.CouponMapper;
import com.example.shop.mapper.UserCouponMapper;
import com.example.shop.service.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CouponTest {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private CouponService couponService;

    @Test
    public void testMapperLoading() {
        // 简单测试Mapper是否能正常加载
        System.out.println("CouponMapper loaded: " + (couponMapper != null));
        System.out.println("UserCouponMapper loaded: " + (userCouponMapper != null));
        System.out.println("CouponService loaded: " + (couponService != null));
    }
}
