package com.okayji.feed.controller;

import com.okayji.common.ApiResponse;
import com.okayji.feed.dto.response.FeedResponse;
import com.okayji.feed.service.FeedService;
import com.okayji.identity.entity.User;
import com.okayji.utils.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/feed")
@AllArgsConstructor
@Tag(name = "Feed Controller")
public class FeedController {

    private final FeedService feedService;

    @GetMapping
    public ApiResponse<FeedResponse> getFeed(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant cursorTime,
            @RequestParam(required = false) String cursorId,
            @RequestParam(defaultValue = "20") int limit,
            @CurrentUser User currentUser) {

        return ApiResponse.<FeedResponse>builder()
                .success(true)
                .data(feedService.getFeed(currentUser.getId(), limit, cursorTime, cursorId))
                .build();
    }
}
