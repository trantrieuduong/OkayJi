package com.okayji.chat.service;

import com.okayji.chat.dto.request.CreateGroupChatRequest;
import com.okayji.chat.dto.request.UpdateGroupChatRequest;
import com.okayji.chat.dto.response.ChatMemberResponse;
import com.okayji.chat.dto.response.ChatResponse;
import com.okayji.chat.dto.response.ListMessageResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ChatService {
    void createDirectChat(String thisUserId, String withUserId);
    void leaveGroupChat(String userId, String groupId);
    Long unreadCount(String userId);
    ChatResponse createGroupChat(String userId, CreateGroupChatRequest createGroupChatRequest);
    ChatResponse updateGroupChat(String groupId, UpdateGroupChatRequest updateGroupChatRequest);
    Page<ChatResponse> getChats(String userId, int page, int size);
    ChatResponse getChat(String userId, String chatId);
    List<ChatMemberResponse> getMembers(String chatId);
    ListMessageResponse getMessages(String chatId, int limit, Long cursorSeq);
}
