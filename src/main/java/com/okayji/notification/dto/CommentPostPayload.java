package com.okayji.notification.dto;

import lombok.Getter;

@Getter
public class CommentPostPayload extends NotificationPayload {
    private final String commentId;
    private final String postId;

    public CommentPostPayload(String reviewTitle, String commentId, String postId) {
        super(reviewTitle);
        this.commentId = commentId;
        this.postId = postId;
    }
}
