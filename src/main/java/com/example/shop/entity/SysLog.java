package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统日志实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_log")
public class SysLog {

    /**
     * 日志ID
     */
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 操作类型
     */
    @TableField("operation")
    private String operation;

    /**
     * 模块名称
     */
    @TableField("module")
    private String module;

    /**
     * 动作类型
     */
    @TableField("action")
    private String action;

    /**
     * 完整方法签名
     */
    @TableField("method")
    private String method;

    /**
     * 参数
     */
    @TableField("params")
    private String params;

    /**
     * IP地址
     */
    @TableField("ip")
    private String ip;

    /**
     * 状态(0-失败,1-成功)
     */
    @TableField("status")
    private Integer status;

    /**
     * 错误消息
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 执行时长(毫秒)
     */
    @TableField("execute_time")
    private Long executeTime;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
