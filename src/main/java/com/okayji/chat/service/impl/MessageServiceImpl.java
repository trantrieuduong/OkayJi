package com.okayji.chat.service.impl;

import com.okayji.chat.dto.request.MessageRequest;
import com.okayji.chat.dto.response.ChatUpdateEvent;
import com.okayji.chat.entity.Chat;
import com.okayji.chat.entity.ChatMember;
import com.okayji.chat.entity.Message;
import com.okayji.chat.repository.ChatMemberRepository;
import com.okayji.chat.repository.ChatRepository;
import com.okayji.chat.repository.MessageRepository;
import com.okayji.chat.service.MessageService;
import com.okayji.chat.entity.ChatEvent;
import com.okayji.exception.AppError;
import com.okayji.exception.AppException;
import com.okayji.identity.entity.User;
import com.okayji.identity.repository.UserRepository;
import com.okayji.mapper.MessageMapper;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static org.apache.commons.lang3.math.NumberUtils.min;

@Service
@AllArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void sendMessage(String chatId, String userId, MessageRequest messageRequest) {
        User sender = userRepository.findUserById(userId);
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(AppError.CHAT_NOT_FOUND));
        ChatMember chatMember = chatMemberRepository
                .findByChat_IdAndMember_Id(chatId, userId)
                .orElseThrow(() -> new AppException(AppError.UNAUTHORIZED));

        Message message = Message.builder()
                .type(messageRequest.getType())
                .content(messageRequest.getContent())
                .sender(sender)
                .chat(chat)
                .seq(chat.getLastMessageSeq() + 1)
                .build();
        messageRepository.saveAndFlush(message);

        chat.setLastMessageAt(Instant.now());
        chat.setLastMessageSeq(message.getSeq());
        chatRepository.save(chat);

        chatMember.setLastReadSeq(message.getSeq());
        chatMemberRepository.save(chatMember);

        messagingTemplate.convertAndSend(
                "/topic/chats/" + chatId + "/messages",
                messageMapper.toMessageResponse(message)
        );
        chatMemberRepository.findChatMembersByChat_Id(chatId).forEach(cm -> {
            User member = cm.getMember();
            messagingTemplate.convertAndSendToUser(
                    member.getId(),
                    "/queue/chats",
                    ChatUpdateEvent.builder()
                            .chatEvent(ChatEvent.NEW_MESSAGE)
                            .chatId(chatId)
                            .messageSeq(message.getSeq())
                            .build()
            );
        });
    }

    @Override
    public void markAsRead(String chatId, String userId, Long messageSeq) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(AppError.CHAT_NOT_FOUND));

        ChatMember chatMember = chatMemberRepository
                .findByChat_IdAndMember_Id(chatId, userId)
                .orElseThrow(() -> new AppException(AppError.UNAUTHORIZED));

        if (chatMember.getLastReadSeq() >= messageSeq)
            return;

        chatMember.setLastReadSeq(min(chat.getLastMessageSeq(), messageSeq));
        chatMemberRepository.save(chatMember);
    }
}
