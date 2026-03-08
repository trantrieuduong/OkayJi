package com.okayji.notification.dto;

import lombok.Getter;

@Getter
public class NewFriendPayload extends NotificationPayload {
    private final String friendId;

    public NewFriendPayload(String reviewTitle, String friendId) {
        super(reviewTitle);
        this.friendId = friendId;
    }
}
