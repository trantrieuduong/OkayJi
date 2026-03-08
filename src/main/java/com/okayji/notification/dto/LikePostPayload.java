package com.okayji.notification.dto;

import lombok.Getter;

@Getter
public class LikePostPayload extends NotificationPayload {
    private final String postId;

    public LikePostPayload(String reviewTitle, String postId) {
        super(reviewTitle);
        this.postId = postId;
    }
}
