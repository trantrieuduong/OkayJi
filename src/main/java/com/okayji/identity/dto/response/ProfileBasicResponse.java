package com.okayji.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileBasicResponse {
    String userId;
    String username;
    String fullName;
    String avatarUrl;
}
