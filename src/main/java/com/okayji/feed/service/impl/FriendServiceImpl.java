package com.okayji.feed.service.impl;

import com.okayji.exception.AppError;
import com.okayji.exception.AppException;
import com.okayji.feed.entity.Friend;
import com.okayji.feed.entity.FriendRequest;
import com.okayji.feed.repository.FriendRepository;
import com.okayji.feed.repository.FriendRequestRepository;
import com.okayji.feed.service.FriendService;
import com.okayji.identity.dto.response.ProfileResponse;
import com.okayji.identity.entity.User;
import com.okayji.identity.repository.UserRepository;
import com.okayji.utils.PairUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void createFriendRequest(String toUserIdOrUsername) {
        User sender = getCurrentUser();
        User receiver = userRepository.findUserByIdOrUsername(toUserIdOrUsername, toUserIdOrUsername)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        try {
            var pair = PairUser.canonical(sender, receiver);

            if (friendRepository.existsByUserLow_IdAndUserHigh_Id(
                    pair.getLow().getId(),
                    pair.getHigh().getId()))
                throw new AppException(AppError.FRIEND_ALREADY);

            if (friendRequestRepository.existsBySender_IdAndReceiver_Id(
                    receiver.getId(),
                    sender.getId()))
                throw new AppException(AppError.FRIEND_REQUEST_EXISTS);

            friendRequestRepository.save(FriendRequest.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .build());
        }
        catch (IllegalArgumentException e) {
            throw new AppException(AppError.FRIEND_YOURSELF);
        }
    }

    @Override
    @Transactional
    public void acceptFriendRequest(String friendRequestId) {
        FriendRequest friendRequest = friendRequestRepository.findById(friendRequestId)
                .orElseThrow(() -> new AppException(AppError.FRIEND_REQUEST_NOT_FOUND));

        User currentUser = getCurrentUser();
        User sender = friendRequest.getSender();
        User receiver = friendRequest.getReceiver();

        if (!receiver.getId().equals(currentUser.getId()))
            throw new AppException(AppError.UNAUTHORIZED);

        var pair = PairUser.canonical(sender, receiver);

        friendRepository.saveAndFlush(Friend.builder()
                .userLow(pair.getLow())
                .userHigh(pair.getHigh())
                .build());
        friendRequestRepository.delete(friendRequest);
    }

    @Override
    public void declineFriendRequest(String friendRequestId) {
        FriendRequest friendRequest = friendRequestRepository.findById(friendRequestId)
                .orElseThrow(() -> new AppException(AppError.FRIEND_REQUEST_NOT_FOUND));

        User currentUser = getCurrentUser();
        User receiver = friendRequest.getReceiver();

        if (!receiver.getId().equals(currentUser.getId()))
            throw new AppException(AppError.UNAUTHORIZED);

        friendRequestRepository.delete(friendRequest);
    }

    @Override
    public void cancelFriendRequest(String friendRequestId) {
        FriendRequest friendRequest = friendRequestRepository.findById(friendRequestId)
                .orElseThrow(() -> new AppException(AppError.FRIEND_REQUEST_NOT_FOUND));

        User currentUser = getCurrentUser();
        User sender = friendRequest.getSender();

        if (!sender.getId().equals(currentUser.getId()))
            throw new AppException(AppError.UNAUTHORIZED);

        friendRequestRepository.delete(friendRequest);
    }

    @Override
    public void unfriend(String anotherUserIdOrUsername) {
        User anotherUser = userRepository.findUserByIdOrUsername(anotherUserIdOrUsername, anotherUserIdOrUsername)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        User currentUser = getCurrentUser();

        var pair = PairUser.canonical(anotherUser, currentUser);
        Friend friend = friendRepository.findByUserLowAndUserHigh(pair.getLow(), pair.getHigh());

        if (Objects.isNull(friend))
            throw new AppException(AppError.NOT_FRIEND);

        friendRepository.delete(friend);
    }

    @Override
    public List<ProfileResponse> getFriends() {
        return List.of();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }
}
