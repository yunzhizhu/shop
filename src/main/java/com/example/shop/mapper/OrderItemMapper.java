package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shop.entity.OrderItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单商品Mapper接口
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 根据订单ID查询订单商品列表
     */
    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据订单编号查询订单商品列表
     */
    List<OrderItem> selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 批量插入订单商品
     */
    int insertBatch(@Param("items") List<OrderItem> items);

    /**
     * 根据订单ID删除订单商品
     */
    int deleteByOrderId(@Param("orderId") Long orderId);

    /**
     * 获取订单商品数量
     */
    Integer countByOrderId(@Param("orderId") Long orderId);
}
