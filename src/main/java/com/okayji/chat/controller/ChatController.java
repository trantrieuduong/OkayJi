package com.okayji.chat.controller;

import com.okayji.chat.dto.request.CreateGroupChatRequest;
import com.okayji.chat.dto.request.UpdateGroupChatRequest;
import com.okayji.chat.dto.response.ChatMemberResponse;
import com.okayji.chat.dto.response.ChatResponse;
import com.okayji.chat.dto.response.ListMessageResponse;
import com.okayji.chat.service.ChatService;
import com.okayji.common.ApiResponse;
import com.okayji.identity.entity.User;
import com.okayji.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chats")
@AllArgsConstructor
@Tag(name = "Chat Controller")
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    @Operation(summary = "Get user chats list")
    ApiResponse<Page<ChatResponse>> getMyChats(@RequestParam(defaultValue = "0") int page, 
                                               @RequestParam(defaultValue = "20") int size,
                                               @CurrentUser User currentUser) {
        return ApiResponse.<Page<ChatResponse>>builder()
                .success(true)
                .data(chatService.getChats(currentUser.getId(), page, size))
                .build();
    }

    @GetMapping("/{chatId}")
    @Operation(summary = "Get chat")
    @PreAuthorize("@permissionCheck.canAccessChat(#currentUser.id, #chatId)")
    ApiResponse<ChatResponse> getChat(@PathVariable String chatId,
                                      @CurrentUser User currentUser) {
        return ApiResponse.<ChatResponse>builder()
                .success(true)
                .data(chatService.getChat(currentUser.getId(), chatId))
                .build();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get number of unread chats")
    ApiResponse<Long> getUnreadCount(@CurrentUser User currentUser) {
        return ApiResponse.<Long>builder()
                .success(true)
                .data(chatService.unreadCount(currentUser.getId()))
                .build();
    }

    @GetMapping("/{chatId}/members")
    @Operation(summary = "Get members in chat")
    @PreAuthorize("@permissionCheck.canAccessChat(#currentUser.id, #chatId)")
    ApiResponse<List<ChatMemberResponse>> getMembers(@PathVariable String chatId,
                                                     @CurrentUser User currentUser) {
        return ApiResponse.<List<ChatMemberResponse>>builder()
                .success(true)
                .data(chatService.getMembers(chatId))
                .build();
    }

    @GetMapping("/{chatId}/messages")
    @Operation(summary = "Get messages in chat")
    @PreAuthorize("@permissionCheck.canAccessChat(#currentUser.id, #chatId)")
    ApiResponse<ListMessageResponse> getMessages(@PathVariable String chatId,
                                                 @RequestParam(defaultValue = "20") int limit,
                                                 @RequestParam(required = false) Long cursorSeq,
                                                 @CurrentUser User currentUser) {
        return ApiResponse.<ListMessageResponse>builder()
                .success(true)
                .data(chatService.getMessages(chatId, limit, cursorSeq))
                .build();
    }

    @PostMapping("/group")
    @Operation(summary = "Create group chat with others")
    ApiResponse<ChatResponse> createGroupChat(@Valid @RequestBody CreateGroupChatRequest request,
                                              @CurrentUser User currentUser) {
        return ApiResponse.<ChatResponse>builder()
                .success(true)
                .data(chatService.createGroupChat(currentUser.getId(), request))
                .build();
    }

    @PostMapping("/group/{groupId}/leave")
    @Operation(summary = "Leave group chat")
    @PreAuthorize("@permissionCheck.canAccessChat(#currentUser.id, #groupId)")
    ApiResponse<?> leaveGroupChat(@PathVariable String groupId,
                                  @CurrentUser User currentUser) {
        chatService.leaveGroupChat(currentUser.getId(), groupId);
        return ApiResponse.builder()
                .success(true)
                .build();
    }

    @PutMapping("/group/{groupId}")
    @Operation(summary = "Update group chat information")
    @PreAuthorize("@permissionCheck.canAccessChat(#currentUser.id, #groupId)")
    ApiResponse<ChatResponse> updateGroupChat(@PathVariable String groupId,
                                              @Valid @RequestBody UpdateGroupChatRequest request,
                                              @CurrentUser User currentUser) {
        return ApiResponse.<ChatResponse>builder()
                .success(true)
                .data(chatService.updateGroupChat(groupId, request))
                .build();
    }
}
