package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shop.dto.FriendInfoResponse;
import com.example.shop.entity.FriendRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 好友关系Mapper
 */
@Mapper
public interface FriendRelationMapper extends BaseMapper<FriendRelation> {

    /**
     * 获取用户的好友列表
     */
    List<FriendInfoResponse> selectFriendList(@Param("userId") Long userId);

    /**
     * 获取用户的好友数量
     */
    Integer countFriends(@Param("userId") Long userId);

    /**
     * 检查是否是好友关系
     */
    Integer checkIsFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
