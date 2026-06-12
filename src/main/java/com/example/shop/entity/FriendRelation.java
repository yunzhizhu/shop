package com.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 好友关系实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("friend_relation")
public class FriendRelation {

    /**
     * 关系ID
     */
    @TableId(value = "relation_id", type = IdType.AUTO)
    private Long relationId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 好友ID
     */
    @TableField("friend_id")
    private Long friendId;

    /**
     * 好友备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 是否拉黑(0-否,1-是)
     */
    @TableField("is_blocked")
    private Integer isBlocked;

    /**
     * 成为好友时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
