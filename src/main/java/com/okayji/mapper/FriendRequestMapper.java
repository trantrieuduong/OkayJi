package com.okayji.mapper;

import com.okayji.feed.dto.response.FriendReqResponse;
import com.okayji.feed.entity.FriendRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FriendRequestMapper {
    @Mapping(source = "friendRequest.sender.id", target = "senderId")
    @Mapping(source = "friendRequest.receiver.id", target = "receiverId")
    FriendReqResponse toFrFriendReqResponse(FriendRequest friendRequest);
}
