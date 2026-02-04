package com.okayji.feed.controller;

import com.okayji.common.ApiResponse;
import com.okayji.feed.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/friends")
@AllArgsConstructor
@Tag(name = "Friend Controller")
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/request/{toUserIdOrUsername}")
    @Operation(summary = "Send friend request to another")
    public ApiResponse<?> sendFriendRequest(@PathVariable String toUserIdOrUsername){
        friendService.createFriendRequest(toUserIdOrUsername);
        return ApiResponse.builder()
                .success(true)
                .message("Friend request sent")
                .build();
    }

    @PostMapping("/accept/{friendRequestId}")
    @Operation(summary = "Accept friend request")
    public ApiResponse<?> acceptFriendRequest(@PathVariable String friendRequestId){
        friendService.acceptFriendRequest(friendRequestId);
        return ApiResponse.builder()
                .success(true)
                .message("Friend request accepted")
                .build();
    }

    @PostMapping("/decline/{friendRequestId}")
    @Operation(summary = "Decline friend request")
    public ApiResponse<?> declineFriendRequest(@PathVariable String friendRequestId){
        friendService.declineFriendRequest(friendRequestId);
        return ApiResponse.builder()
                .success(true)
                .message("Friend request declined")
                .build();
    }

    @PostMapping("/cancel/{friendRequestId}")
    @Operation(summary = "Cancel friend request")
    public ApiResponse<?> cancelFriendRequest(@PathVariable String friendRequestId){
        friendService.cancelFriendRequest(friendRequestId);
        return ApiResponse.builder()
                .success(true)
                .message("Friend request cancelled")
                .build();
    }

    @PostMapping("/unfriend/{anotherUserIdOrUsername}")
    @Operation(summary = "Unfriend another by userId or username")
    public ApiResponse<?> unfriend(@PathVariable String anotherUserIdOrUsername){
        friendService.unfriend(anotherUserIdOrUsername);
        return ApiResponse.builder()
                .success(true)
                .message("Unfriend successfully")
                .build();
    }
}
