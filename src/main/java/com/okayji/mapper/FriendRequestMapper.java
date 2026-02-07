package com.okayji.mapper;

import com.okayji.feed.dto.response.FriendReqResponse;
import com.okayji.feed.entity.FriendRequest;
import com.okayji.identity.dto.response.ProfileBasicResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FriendRequestMapper {
    @Mapping(target = "sender", source = "sender")
    @Mapping(target = "receiver", source = "receiver")
    FriendReqResponse toFriendReqResponse(FriendRequest friendRequest,
                                          ProfileBasicResponse sender,
                                          ProfileBasicResponse receiver);
}
