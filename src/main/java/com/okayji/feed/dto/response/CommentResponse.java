package com.okayji.feed.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Setter
@Getter
public class CommentResponse {
    String id;
    String userId;
    String username;
    String userFullName;
    String userAvatarUrl;
    String postId;
    String content;
    Instant createdAt;
}
