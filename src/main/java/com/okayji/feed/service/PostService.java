package com.okayji.feed.service;

import com.okayji.feed.dto.request.PostCreationRequest;
import com.okayji.feed.dto.response.PostResponse;
import org.springframework.data.domain.Page;

public interface PostService {
    PostResponse getPostById(String id);
    PostResponse createPost(PostCreationRequest postCreationRequest);
    void deletePostById(String id);
    Page<PostResponse> getPostsByUser(String userIdOrUsername, int page, int size);
}
