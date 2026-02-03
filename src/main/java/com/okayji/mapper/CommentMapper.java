package com.okayji.mapper;

import com.okayji.feed.dto.request.CommentCreationRequest;
import com.okayji.feed.dto.request.CommentUpdateRequest;
import com.okayji.feed.dto.response.CommentResponse;
import com.okayji.feed.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    Comment toComment(CommentCreationRequest commentCreationRequest);

    @Mapping(source = "comment.user.id", target = "userId")
    @Mapping(source = "comment.post.id", target = "postId")
    @Mapping(source = "comment.user.profile.fullName", target = "userFullName")
    @Mapping(source = "comment.user.profile.avatarUrl", target = "userAvatarUrl")
    @Mapping(source = "comment.user.username", target = "username")
    CommentResponse toCommentResponse(Comment comment);

    @Mapping(target = "id", ignore = true)
    void updateComment(@MappingTarget Comment comment, CommentUpdateRequest request);
}
