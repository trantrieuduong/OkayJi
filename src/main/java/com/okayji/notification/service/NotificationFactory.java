package com.okayji.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okayji.feed.entity.PostStatus;
import com.okayji.notification.dto.*;
import com.okayji.notification.entity.NotificationType;
import com.okayji.feed.entity.FriendRequest;
import com.okayji.feed.entity.Post;
import com.okayji.identity.entity.User;
import com.okayji.notification.entity.Notification;

public class NotificationFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Notification friendRequest(FriendRequest friendRequest) {
        return Notification.builder()
                .user(friendRequest.getReceiver())
                .type(NotificationType.FRIEND_REQUEST)
                .payload(toJson(new FriendRequestPayload(
                        friendRequest.getSender().getProfile().getFullName() + " send you a friend request",
                        friendRequest.getId(),
                        friendRequest.getSender().getId())
                ))
                .build();
    }

    public static Notification newFriend(User user, User friend) {
        return Notification.builder()
                .user(user)
                .type(NotificationType.NEW_FRIEND)
                .payload(toJson(new NewFriendPayload(
                        friend.getProfile().getFullName() + " and you is friend now",
                        friend.getId())
                ))
                .build();
    }

    public static Notification likePost(Post post, User liker) {
        return Notification.builder()
                .user(post.getUser())
                .type(NotificationType.LIKE_POST)
                .payload(toJson(new LikePostPayload(
                        liker.getProfile().getFullName() + " like your post",
                        post.getId())
                ))
                .build();
    }

    public static Notification commentPost(User user, User commenter, String commentId, String postId) {
        return Notification.builder()
                .user(user)
                .type(NotificationType.COMMENT_POST)
                .payload(toJson(new CommentPostPayload(
                        commenter.getProfile().getFullName() + " comment your post",
                        commentId,
                        postId)
                ))
                .build();
    }

    /**
     *
     * @param status REJECTED or UNDER_REVIEW
     */
    public static Notification violatedPost(User user, String postId, PostStatus status) {
        if (status != PostStatus.REJECTED && status != PostStatus.UNDER_REVIEW)
            throw new IllegalArgumentException("Post status must be REJECTED or UNDER_REVIEW");

        return Notification.builder()
                .user(user)
                .type(NotificationType.SYSTEM_ANNOUNCEMENT)
                .payload(toJson(new SystemPayload(
                        "Your post violated our policy and has been " +
                                (status.equals(PostStatus.UNDER_REVIEW)
                                        ? "under review by admin."
                                        : status.toString().toLowerCase()
                                ),
                        postId)
                ))
                .build();
    }

    public static Notification violatedComment(User user, String postId) {
        return Notification.builder()
                .user(user)
                .type(NotificationType.SYSTEM_ANNOUNCEMENT)
                .payload(toJson(new SystemPayload(
                        "Your comment violated our policy and has been deleted",
                        postId)
                ))
                .build();
    }

    private static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can not convert to Json", e);
        }
    }
}
