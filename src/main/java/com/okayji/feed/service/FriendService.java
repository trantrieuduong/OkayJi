package com.okayji.feed.service;

import com.okayji.feed.dto.response.FriendReqResponse;
import com.okayji.identity.dto.response.ProfileBasicResponse;

import java.util.List;

public interface FriendService {
    void createFriendRequest(String toUserIdOrUsername);
    void acceptFriendRequest(String friendRequestId);
    void declineFriendRequest(String friendRequestId);
    void cancelFriendRequest(String friendRequestId);
    void unfriend(String anotherUserIdOrUsername);
    List<ProfileBasicResponse> getFriends();
    List<FriendReqResponse> getFriendRequestSent();
    List<FriendReqResponse> getFriendRequestReceived();
}
