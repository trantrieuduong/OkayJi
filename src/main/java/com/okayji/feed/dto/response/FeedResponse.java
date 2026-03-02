package com.okayji.feed.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeedResponse(List<PostResponse> items,
                           Instant nextCursorTime,
                           String nextCursorId) { }
