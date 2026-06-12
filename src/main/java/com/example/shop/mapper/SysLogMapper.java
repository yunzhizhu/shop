package com.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.shop.entity.SysLog;
import org.apache.ibatis.annotations.Param;

/**
 * 系统日志Mapper接口
 */
public interface SysLogMapper extends BaseMapper<SysLog> {

    /**
     * 分页查询日志列表
     */
    IPage<SysLog> selectLogPage(Page<SysLog> page, 
                               @Param("userId") Long userId,
                               @Param("operation") String operation,
                               @Param("module") String module);
}
