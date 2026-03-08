package com.okayji.notification.dto;

import lombok.Getter;

@Getter
public class FriendRequestPayload extends NotificationPayload {
    private final String requestId;
    private final String requesterId;

    public FriendRequestPayload(String reviewTitle, String requestId, String requesterId) {
        super(reviewTitle);
        this.requestId = requestId;
        this.requesterId = requesterId;
    }
}
