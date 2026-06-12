package com.example.shop;

import com.example.shop.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MybatisPlusTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testMapperLoading() {
        // 简单测试Mapper是否能正常加载
        System.out.println("UserMapper loaded: " + (userMapper != null));
    }
}
