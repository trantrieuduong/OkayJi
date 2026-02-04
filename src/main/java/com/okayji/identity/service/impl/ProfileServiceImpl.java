package com.okayji.identity.service.impl;

import com.okayji.exception.AppError;
import com.okayji.exception.AppException;
import com.okayji.feed.entity.FriendRequest;
import com.okayji.feed.repository.FriendRepository;
import com.okayji.feed.repository.FriendRequestRepository;
import com.okayji.identity.dto.request.ProfileUpdateRequest;
import com.okayji.identity.dto.response.ProfileResponse;
import com.okayji.identity.entity.Profile;
import com.okayji.identity.entity.User;
import com.okayji.identity.repository.ProfileRepository;
import com.okayji.identity.repository.UserRepository;
import com.okayji.identity.service.ProfileService;
import com.okayji.mapper.FriendRequestMapper;
import com.okayji.mapper.ProfileMapper;
import com.okayji.utils.PairUser;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@AllArgsConstructor
@Service
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;

    @Override
    public ProfileResponse getUserProfile(String userIdOrUsername) {
        User user = userRepository
                .findUserByIdOrUsername(userIdOrUsername, userIdOrUsername)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        Profile profile = user.getProfile();
        ProfileResponse profileResponse = profileMapper.toProfileResponse(profile);

        User currentUser = getCurrentUser();
        if (!user.getId().equals(currentUser.getId())) {
            var pair = PairUser.canonical(user, currentUser);
            profileResponse.setFriend(friendRepository
                    .existsByUserLow_IdAndUserHigh_Id(pair.getLow().getId(), pair.getHigh().getId()));

            FriendRequest friendRequest = friendRequestRepository.findBySenderAndReceiver(user, currentUser);
            if (Objects.isNull(friendRequest))
                friendRequest = friendRequestRepository.findBySenderAndReceiver(currentUser, user);

            profileResponse.setFriendRequest(Objects.nonNull(friendRequest)
                    ? friendRequestMapper.toFrFriendReqResponse(friendRequest)
                    : null);
        }

        return profileResponse;
    }

    @Override
    public ProfileResponse getMyProfile() {
        return this.getUserProfile(getCurrentUser().getId());
    }

    @Override
    public ProfileResponse updateUserProfile(ProfileUpdateRequest profileUpdateRequest) {
        User user = getCurrentUser();

        Profile profile = user.getProfile();
        profileMapper.updateProfile(profile, profileUpdateRequest);
        profileRepository.save(profile);

        return profileMapper.toProfileResponse(profile);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }
}
