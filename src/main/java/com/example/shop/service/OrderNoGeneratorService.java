package com.example.shop.service;

/**
 * 订单号生成服务接口
 * 使用Redis INCR保证分布式环境下订单号唯一性
 */
public interface OrderNoGeneratorService {

    /**
     * 生成订单号
     * 格式：NO + yyyyMMddHHmmss + 6位序列号
     * 例如：NO20260223143025000001
     * 
     * @return 订单号
     */
    String generateOrderNo();
}
