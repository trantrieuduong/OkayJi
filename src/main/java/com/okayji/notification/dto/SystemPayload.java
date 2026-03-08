package com.okayji.notification.dto;

import lombok.Getter;

@Getter
public class SystemPayload extends NotificationPayload {
    private final String targetId;

    public SystemPayload (String reviewTitle, String targetId) {
        super(reviewTitle);
        this.targetId = targetId;
    }
}
