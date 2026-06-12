package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper接口
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 根据手机号查询用户
     */
    User selectByPhone(@Param("phone") String phone);

    /**
     * 分页查询用户列表
     */
    IPage<User> selectUserPage(Page<User> page, @Param("username") String username, @Param("roleId") Integer roleId);

    /**
     * 更新用户最后登录时间
     */
    int updateLastLoginTime(@Param("userId") Long userId);

    /**
     * 获取所有活跃用户ID列表
     */
    List<Long> selectAllActiveUserIds();
}
