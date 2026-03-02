package com.okayji.feed.service;

import com.okayji.feed.dto.response.FeedResponse;

import java.time.Instant;

public interface FeedService {
    FeedResponse getFeed(String viewerId, int limit, Instant cursorTime, String cursorId);
}
