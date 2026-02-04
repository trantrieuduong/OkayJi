package com.okayji.feed.service;

import com.okayji.identity.dto.response.ProfileResponse;

import java.util.List;

public interface FriendService {
    void createFriendRequest(String toUserIdOrUsername);
    void acceptFriendRequest(String friendRequestId);
    void declineFriendRequest(String friendRequestId);
    void cancelFriendRequest(String friendRequestId);
    void unfriend(String anotherUserIdOrUsername);
    List<ProfileResponse> getFriends();
}
