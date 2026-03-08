package com.okayji.feed.service.impl;

import com.okayji.exception.AppError;
import com.okayji.exception.AppException;
import com.okayji.feed.dto.request.PostCreationRequest;
import com.okayji.feed.dto.request.PostUpdateRequest;
import com.okayji.feed.dto.response.PostResponse;
import com.okayji.feed.entity.Post;
import com.okayji.feed.entity.PostMedia;
import com.okayji.feed.entity.PostStatus;
import com.okayji.feed.repository.CommentRepository;
import com.okayji.feed.repository.ReactionRepository;
import com.okayji.file.service.S3Service;
import com.okayji.identity.entity.User;
import com.okayji.feed.repository.PostRepository;
import com.okayji.feed.service.PostService;
import com.okayji.identity.repository.UserRepository;
import com.okayji.mapper.PostMapper;
import com.okayji.moderation.event.PostModerationEvent;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostRepository postRepository;
    private final ReactionRepository reactionRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final S3Service s3Service;

    @Override
    public PostResponse getPostById(String viewerId, String id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(AppError.POST_NOT_FOUND));

        return postMapper.toPostResponse(post,
                reactionRepository.existsByPostIdAndUserId(post.getId(), viewerId),
                reactionRepository.countByPost_Id(post.getId()),
                commentRepository.countByPost_Id(post.getId())
        );
    }

    @Override
    @Transactional
    public PostResponse createPost(String userId, PostCreationRequest postCreationRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        Post post = postMapper.toPost(postCreationRequest, user);
        post.setStatus(PostStatus.PENDING);

        postCreationRequest.getMedia().forEach(media -> {
            PostMedia postMedia = PostMedia.builder()
                    .post(post)
                    .type(media.getType())
                    .mediaUrl(media.getMediaUrl())
                    .build();
            post.getPostMedia().add(postMedia);
        });

        postRepository.saveAndFlush(post);
        entityManager.refresh(post);

        applicationEventPublisher.publishEvent(new PostModerationEvent(post.getId()));
        return postMapper.toPostResponse(post);
    }

    @Override
    public PostResponse updatePost(String postId, PostUpdateRequest postUpdateRequest) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(AppError.POST_NOT_FOUND));

        postMapper.updatePost(post, postUpdateRequest);
        post.setStatus(PostStatus.PENDING);
        postRepository.save(post);

        applicationEventPublisher.publishEvent(new PostModerationEvent(post.getId()));
        return postMapper.toPostResponse(post);
    }

    @Override
    public void deletePostById(String id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(AppError.POST_NOT_FOUND));

        post.getPostMedia().forEach(media -> s3Service.deleteObject(media.getMediaUrl()));
        postRepository.delete(post);
    }

    @Override
    public Page<PostResponse> getPostsByUser(String viewerId, String userIdOrUsername, int page, int size) {
        User user = userRepository.findUserByIdOrUsername(userIdOrUsername, userIdOrUsername)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        Pageable pageable = PageRequest
                .of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Post> postPage;
        if (user.getId().equals(viewerId))
            postPage = postRepository.findByUser_Id(user.getId(), pageable);
        else
            postPage = postRepository.findPublishedPostsByUser_Id(user.getId(), pageable);

        return postPage.map(post -> postMapper
                .toPostResponse(post,
                        reactionRepository.existsByPostIdAndUserId(post.getId(), viewerId),
                        reactionRepository.countByPost_Id(post.getId()),
                        commentRepository.countByPost_Id(post.getId())
                )
        );
    }
}
