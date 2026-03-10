package com.okayji.chat.service.impl;

import com.okayji.chat.dto.request.CreateGroupChatRequest;
import com.okayji.chat.dto.request.UpdateGroupChatRequest;
import com.okayji.chat.dto.response.ChatMemberResponse;
import com.okayji.chat.dto.response.ChatResponse;
import com.okayji.chat.dto.response.MessageResponse;
import com.okayji.chat.entity.Chat;
import com.okayji.chat.entity.ChatMember;
import com.okayji.chat.repository.ChatMemberRepository;
import com.okayji.chat.repository.ChatRepository;
import com.okayji.chat.repository.MessageRepository;
import com.okayji.chat.service.ChatService;
import com.okayji.chat.entity.ChatType;
import com.okayji.exception.AppError;
import com.okayji.exception.AppException;
import com.okayji.feed.repository.FriendRepository;
import com.okayji.file.service.S3MediaTypes;
import com.okayji.file.service.S3Service;
import com.okayji.identity.entity.Profile;
import com.okayji.identity.entity.User;
import com.okayji.identity.repository.UserRepository;
import com.okayji.mapper.ChatMapper;
import com.okayji.mapper.MessageMapper;
import com.okayji.utils.PairUser;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final ChatMapper chatMapper;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final S3Service s3Service;

    @Override
    @Transactional
    public void createDirectChat(String thisUserId, String withUserId) {
        User currentUser = userRepository.findById(thisUserId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        User otherUser = userRepository.findById(withUserId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        PairUser pairUser = PairUser.canonical(currentUser, otherUser);
        String directKey = pairUser.makeKey();
        Chat chat = chatRepository.findByDirectKey(directKey);

        if (Objects.isNull(chat)) {
            if (!friendRepository.existsByUserLow_IdAndUserHigh_Id(pairUser.getLow().getId(),
                    pairUser.getHigh().getId()))
                throw new AppException(AppError.NOT_FRIEND);

            chat = Chat.builder()
                    .type(ChatType.DIRECT)
                    .createdBy(currentUser)
                    .directKey(directKey)
                    .build();
            chatRepository.saveAndFlush(chat);

            ChatMember chatMember1 = ChatMember.builder()
                    .chat(chat)
                    .member(currentUser)
                    .build();
            ChatMember chatMember2 = ChatMember.builder()
                    .chat(chat)
                    .member(otherUser)
                    .build();
            chatMemberRepository.saveAllAndFlush(List.of(chatMember1, chatMember2));
        }
        chatMapper.toChatResponse(
                chat,
                chatRepository.unreadCount(currentUser.getId(), chat.getId()),
                otherUser.getProfile().getAvatarUrl(),
                otherUser.getProfile().getFullName()
        );
    }

    @Override
    @Transactional
    public void leaveGroupChat(String userId, String groupId) {
        ChatMember member = chatMemberRepository.findByChat_IdAndMember_Id(groupId, userId)
                .orElseThrow(() -> new AppException(AppError.CHAT_NOT_FOUND));

        if (member.getChat().getType() != ChatType.GROUP)
            return;

        chatMemberRepository.delete(member);
    }

    @Override
    public Long unreadCount(String userId) {
        return chatRepository.unreadCount(userId);
    }

    @Override
    @Transactional
    public ChatResponse createGroupChat(String userId, CreateGroupChatRequest createGroupChatRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        if (createGroupChatRequest.getChatAvatarUrl() != null) {
            String avatarUrlContentType = s3Service
                    .getContentTypeFromS3Url(createGroupChatRequest.getChatAvatarUrl());

            if (!S3MediaTypes.isImageType(avatarUrlContentType))
                throw new AppException(AppError.INVALID_INPUT_DATA);
        }

        Chat chat = Chat.builder()
                .type(ChatType.GROUP)
                .chatName(createGroupChatRequest.getChatName())
                .chatAvatarUrl(createGroupChatRequest.getChatAvatarUrl())
                .createdBy(user)
                .build();
        chatRepository.saveAndFlush(chat);

        List<ChatMember> chatMembers = new ArrayList<>();
        chatMembers.add(ChatMember.builder()
                .chat(chat)
                .member(user).build());

        createGroupChatRequest.getMemberIds().forEach(memberId -> {
            User other = userRepository.findById(memberId)
                    .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

            PairUser pairUser = PairUser.canonical(user, other);
            if (!friendRepository.existsByUserLow_IdAndUserHigh_Id(
                    pairUser.getLow().getId(),
                    pairUser.getHigh().getId()))
                throw new AppException(AppError.NOT_FRIEND);

            chatMembers.add(ChatMember.builder()
                    .chat(chat)
                    .member(other).build());
        });

        chatMemberRepository.saveAll(chatMembers);
        return chatMapper.toChatResponse(chat, 0L);
    }

    @Override
    public ChatResponse updateGroupChat(String groupId,
                                        UpdateGroupChatRequest updateGroupChatRequest) {
        Chat chat = chatRepository.findById(groupId)
                .orElseThrow(() -> new AppException(AppError.CHAT_NOT_FOUND));

        if (chat.getType() != ChatType.GROUP)
            throw new AppException(AppError.INVALID_INPUT_DATA);

        chat.setChatName(updateGroupChatRequest.getChatName());

        if (updateGroupChatRequest.getChatAvatarUrl() != null) {
            String avatarUrlContentType = s3Service
                    .getContentTypeFromS3Url(updateGroupChatRequest.getChatAvatarUrl());

            if (!S3MediaTypes.isImageType(avatarUrlContentType))
                throw new AppException(AppError.INVALID_INPUT_DATA);

            chat.setChatAvatarUrl(updateGroupChatRequest.getChatAvatarUrl());
        }

        chatRepository.save(chat);
        return chatMapper.toChatResponse(
                chat,
                0
        );
    }

    @Override
    public Page<ChatResponse> getChats(String userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        Pageable pageable = PageRequest.of(page, size);

        return chatRepository.findMyChatsOrderByLastMessageAt(user.getId(), pageable)
                .map(chat -> getChatResponse(chat, user));
    }

    @Override
    public ChatResponse getChat(String userId, String chatId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(AppError.CHAT_NOT_FOUND));

        return getChatResponse(chat, user);
    }

    @Override
    public List<ChatMemberResponse> getMembers(String chatId) {
        return chatMemberRepository.findChatMembersByChat_Id(chatId).stream()
                .map(chatMapper::toChatMemberResponse)
                .toList();
    }

    @Override
    public Page<MessageResponse> getMessages(String chatId, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "seq");
        Pageable pageable = PageRequest.of(page, size, sort);

        return messageRepository.findByChatId(chatId, pageable)
                .map(messageMapper::toMessageResponse);
    }

    private ChatResponse getChatResponse(Chat chat, User currentUser) {
        if (chat.getType() == ChatType.DIRECT) {
            Profile other = Objects.requireNonNull(chatMemberRepository
                            .getOtherInDirectChat(chat.getId(), currentUser.getId()))
                    .getMember()
                    .getProfile();

            return chatMapper.toChatResponse(
                    chat,
                    chatRepository.unreadCount(currentUser.getId(), chat.getId()),
                    other.getAvatarUrl(),
                    other.getFullName()
            );
        }
        return chatMapper.toChatResponse(
                chat,
                chatRepository.unreadCount(currentUser.getId(), chat.getId())
        );
    }
}
