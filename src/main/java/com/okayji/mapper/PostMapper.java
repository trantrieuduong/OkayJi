package com.okayji.mapper;

import com.okayji.feed.dto.request.PostCreationRequest;
import com.okayji.feed.dto.response.PostResponse;
import com.okayji.feed.entity.Post;
import com.okayji.identity.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMapper {
    @Mapping(source = "post.user.id", target = "userId")
    @Mapping(source = "post.user.profile.fullName", target = "userFullName")
    @Mapping(source = "post.user.profile.avatarUrl", target = "userAvatarUrl")
    @Mapping(source = "post.user.username", target = "username")
    PostResponse toPostResponse(Post post, boolean liked, long likesCount,  long commentsCount);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(source = "user.id", target = "user.id")
    Post toPost(PostCreationRequest postCreationRequest, User user);
}
