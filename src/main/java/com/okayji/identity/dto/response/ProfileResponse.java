package com.okayji.identity.dto.response;

import com.okayji.enums.Gender;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Setter
@Getter
public class ProfileResponse {
    String userId;
    String username;
    String fullName;
    Gender gender;
    String bio;
    LocalDate birthday;
    String avatarUrl;
    String coverImageUrl;
}
