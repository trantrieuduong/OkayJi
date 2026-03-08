package com.okayji.feed.service.impl;

import com.okayji.feed.dto.response.FeedResponse;
import com.okayji.feed.dto.response.PostResponse;
import com.okayji.feed.entity.Post;
import com.okayji.feed.repository.FriendRepository;
import com.okayji.feed.repository.PostRepository;
import com.okayji.feed.service.FeedService;
import com.okayji.feed.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService {

    private final FriendRepository friendRepository;
    private final PostRepository postRepository;
    private final PostService postService;

    @Override
    public FeedResponse getFeed(String viewerId, int limit, Instant cursorTime, String cursorId) {
        List<String> friendIds = friendRepository
                .findByUserLow_IdOrUserHigh_Id(viewerId, viewerId).stream()
                .map(friend -> friend.getUserLow().getId().equals(viewerId)
                        ? friend.getUserHigh().getId()
                        : friend.getUserLow().getId()
                )
                .toList();

        Pageable pageable = PageRequest.of(0, limit);

        Slice<Post> slice = (cursorTime == null || cursorId == null)
                ? postRepository.findFeedFirstPage(friendIds, pageable)
                : postRepository.findFeedAfterCursor(friendIds, cursorTime, cursorId, pageable);

        List<Post> posts = slice.getContent();
        List<PostResponse> postResponseList = posts.stream()
                .map(post -> postService.getPostById(viewerId, post.getId())).toList();

        String nextCursorId = null;
        Instant nextCursorTime = null;
        if (!postResponseList.isEmpty() && slice.hasNext()) {
            Post last = posts.getLast();
            nextCursorId = last.getId();
            nextCursorTime = last.getCreatedAt();
        }

        return FeedResponse.builder()
                .items(postResponseList)
                .nextCursorId(nextCursorId)
                .nextCursorTime(nextCursorTime)
                .build();
    }
}
