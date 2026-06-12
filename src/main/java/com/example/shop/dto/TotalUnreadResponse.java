package com.example.shop.dto;

import lombok.Data;

/**
 * 总未读数响应DTO
 */
@Data
public class TotalUnreadResponse {

    /**
     * 总未读消息数（包含好友请求）
     */
    private Integer totalUnreadCount;

    /**
     * 私信未读数
     */
    private Integer privateUnreadCount;

    /**
     * 系统通知未读数
     */
    private Integer systemUnreadCount;

    /**
     * 好友请求未读数
     */
    private Integer friendRequestCount;

    public TotalUnreadResponse() {
        this.totalUnreadCount = 0;
        this.privateUnreadCount = 0;
        this.systemUnreadCount = 0;
        this.friendRequestCount = 0;
    }

    public TotalUnreadResponse(Integer totalUnreadCount, Integer privateUnreadCount, Integer systemUnreadCount, Integer friendRequestCount) {
        this.totalUnreadCount = totalUnreadCount != null ? totalUnreadCount : 0;
        this.privateUnreadCount = privateUnreadCount != null ? privateUnreadCount : 0;
        this.systemUnreadCount = systemUnreadCount != null ? systemUnreadCount : 0;
        this.friendRequestCount = friendRequestCount != null ? friendRequestCount : 0;
    }
}