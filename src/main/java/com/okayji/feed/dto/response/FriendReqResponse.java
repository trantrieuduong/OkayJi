package com.okayji.feed.dto.response;

import com.okayji.identity.dto.response.ProfileBasicResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Setter
@Getter
public class FriendReqResponse {
    String id;
    ProfileBasicResponse sender;
    ProfileBasicResponse receiver;
    Instant createdAt;
}
