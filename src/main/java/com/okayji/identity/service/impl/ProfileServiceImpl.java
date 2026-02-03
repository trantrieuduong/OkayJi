package com.okayji.identity.service.impl;

import com.okayji.exception.AppError;
import com.okayji.exception.AppException;
import com.okayji.identity.dto.request.ProfileUpdateRequest;
import com.okayji.identity.dto.response.ProfileResponse;
import com.okayji.identity.entity.Profile;
import com.okayji.identity.entity.User;
import com.okayji.identity.repository.ProfileRepository;
import com.okayji.identity.repository.UserRepository;
import com.okayji.identity.service.ProfileService;
import com.okayji.mapper.ProfileMapper;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;

    @Override
    public ProfileResponse getUserProfile(String userIdOrUsername) {
        Profile profile = userRepository
                .findUserByIdOrUsername(userIdOrUsername, userIdOrUsername)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND))
                .getProfile();

        return profileMapper.toProfileResponse(profile);
    }

    @Override
    public ProfileResponse getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        return this.getUserProfile(user.getId());
    }

    @Override
    public ProfileResponse updateUserProfile(ProfileUpdateRequest profileUpdateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        Profile profile = user.getProfile();
        profileMapper.updateProfile(profile, profileUpdateRequest);
        profileRepository.save(profile);

        return profileMapper.toProfileResponse(profile);
    }
}
