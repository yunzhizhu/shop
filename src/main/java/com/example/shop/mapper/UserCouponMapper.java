package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shop.dto.UserCouponResponse;
import com.example.shop.entity.UserCoupon;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户优惠券Mapper接口
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 检查用户是否已领取该优惠券
     */
    UserCoupon selectByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 获取用户优惠券列表
     */
    List<UserCouponResponse> selectUserCoupons(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 获取用户可用的优惠券
     */
    List<UserCouponResponse> selectAvailableUserCoupons(@Param("userId") Long userId, 
                                                       @Param("currentTime") LocalDateTime currentTime);

    /**
     * 使用优惠券
     */
    int useCoupon(@Param("userCouponId") Long userCouponId,
                  @Param("useTime") LocalDateTime useTime);

    /**
     * 退还优惠券
     */
    int returnCoupon(@Param("userCouponId") Long userCouponId);

    /**
     * 更新过期优惠券状态
     */
    int updateExpiredCoupons(@Param("currentTime") LocalDateTime currentTime);


}
