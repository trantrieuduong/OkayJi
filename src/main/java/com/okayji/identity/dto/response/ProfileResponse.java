package com.okayji.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.okayji.enums.Gender;
import com.okayji.feed.dto.response.FriendReqResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {
    String userId;
    String username;
    String fullName;
    Gender gender;
    String bio;
    LocalDate birthday;
    String avatarUrl;
    String coverImageUrl;
    boolean isFriend;
    FriendReqResponse friendRequest;
}
