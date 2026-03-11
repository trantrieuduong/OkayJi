package com.okayji.feed.service.impl;

import com.okayji.exception.AppError;
import com.okayji.exception.AppException;
import com.okayji.feed.dto.request.CommentCreationRequest;
import com.okayji.feed.dto.request.CommentUpdateRequest;
import com.okayji.feed.dto.response.CommentResponse;
import com.okayji.feed.entity.Comment;
import com.okayji.feed.entity.Post;
import com.okayji.feed.repository.CommentRepository;
import com.okayji.feed.repository.PostRepository;
import com.okayji.feed.service.CommentService;
import com.okayji.identity.entity.User;
import com.okayji.identity.repository.UserRepository;
import com.okayji.mapper.CommentMapper;
import com.okayji.moderation.entity.ModerationJob;
import com.okayji.moderation.entity.TargetType;
import com.okayji.moderation.repository.ModerationJobRepository;
import com.okayji.notification.service.NotificationService;
import com.okayji.notification.service.NotificationFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final PostRepository postRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ModerationJobRepository moderationJobRepository;

    @Override
    public CommentResponse createComment(String userId, CommentCreationRequest request) {
        String postId = request.getPostId();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(AppError.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        Comment comment = commentMapper.toComment(request);
        comment.setUser(user);
        comment.setPost(post);
        commentRepository.save(comment);

        // if commenter not post owner -> ping noti to post owner
        if (!post.getUser().getId().equals(user.getId()))
            notificationService.sendNotification(NotificationFactory
                    .commentPost(
                            post.getUser(),
                            user,
                            comment.getId(),
                            postId
                    ));

        moderationJobRepository.save(ModerationJob
                .builder()
                .targetId(comment.getId())
                .targetType(TargetType.COMMENT)
                .build());
        return commentMapper.toCommentResponse(comment);
    }

    @Override
    public CommentResponse updateComment(String commentId, CommentUpdateRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(AppError.COMMENT_NOT_FOUND));

        commentMapper.updateComment(comment, request);
        commentRepository.save(comment);

        moderationJobRepository.save(ModerationJob
                .builder()
                .targetId(commentId)
                .targetType(TargetType.COMMENT)
                .build());
        return commentMapper.toCommentResponse(comment);
    }

    @Override
    public void deleteComment(String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(AppError.COMMENT_NOT_FOUND));

        commentRepository.delete(comment);
    }

    @Override
    public Page<CommentResponse> getCommentsByPostId(String postId, int page, int size) {
        Pageable pageable = PageRequest
                .of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return commentRepository.findByPost_Id(postId, pageable)
                .map(commentMapper::toCommentResponse);
    }
}
