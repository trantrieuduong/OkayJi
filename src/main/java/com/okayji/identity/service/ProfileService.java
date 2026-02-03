package com.okayji.identity.service;

import com.okayji.identity.dto.request.ProfileUpdateRequest;
import com.okayji.identity.dto.response.ProfileResponse;

public interface ProfileService {
    ProfileResponse getUserProfile(String userIdOrUsername);
    ProfileResponse getMyProfile();
    ProfileResponse updateUserProfile(ProfileUpdateRequest profileUpdateRequest);
}
