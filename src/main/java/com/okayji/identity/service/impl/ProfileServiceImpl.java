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
    public ProfileResponse getUserProfile(String viewerId, String userIdOrUsername) {
        User user = userRepository.findUserByIdOrUsername(userIdOrUsername, userIdOrUsername)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        Profile profile = user.getProfile();
        ProfileResponse profileResponse = profileMapper.toProfileResponse(profile);
        if (user.getId().equals(viewerId))
            return profileResponse;

        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        // if friend
        var pair = PairUser.canonical(user, viewer);
        if (friendRepository.existsByUserLow_IdAndUserHigh_Id(pair.getLow().getId(), pair.getHigh().getId())) {
            profileResponse.setFriend(true);
            return profileResponse;
        }

        // if friend request exist
        FriendRequest friendRequest = friendRequestRepository.findBySenderAndReceiver(user, viewer);
        if (friendRequest == null)
            friendRequest = friendRequestRepository.findBySenderAndReceiver(viewer, user);
        profileResponse.setFriendRequest(
                Objects.nonNull(friendRequest)
                ? friendRequestMapper.toFriendReqResponse(
                        friendRequest,
                        profileMapper.toProfileBasicResponse(friendRequest.getSender().getProfile()),
                        profileMapper.toProfileBasicResponse(friendRequest.getReceiver().getProfile())
                )
                : null
        );
        return profileResponse;
    }


    @Override
    public ProfileResponse updateUserProfile(String userId, ProfileUpdateRequest profileUpdateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        Profile profile = user.getProfile();
        profileMapper.updateProfile(profile, profileUpdateRequest);
        profileRepository.save(profile);
        return profileMapper.toProfileResponse(profile);
    }
}
