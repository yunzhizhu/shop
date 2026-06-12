package com.example.shop.enums;

import lombok.Getter;

/**
 * 好友请求状态枚举
 */
@Getter
public enum FriendRequestStatus {
    PENDING(0, "待处理"),
    ACCEPTED(1, "已接受"),
    REJECTED(2, "已拒绝"),
    EXPIRED(3, "已过期");

    private final Integer code;
    private final String description;

    FriendRequestStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static FriendRequestStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FriendRequestStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
