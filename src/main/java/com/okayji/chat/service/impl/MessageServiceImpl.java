package com.okayji.chat.service.impl;

import com.okayji.chat.dto.request.MessageRequest;
import com.okayji.chat.dto.response.ChatUpdateEvent;
import com.okayji.chat.entity.*;
import com.okayji.chat.repository.ChatMemberRepository;
import com.okayji.chat.repository.ChatRepository;
import com.okayji.chat.repository.MessageRepository;
import com.okayji.chat.service.MessageService;
import com.okayji.exception.AppError;
import com.okayji.exception.AppException;
import com.okayji.file.service.S3MediaTypes;
import com.okayji.file.service.S3Service;
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
    private final S3Service s3Service;

    @Override
    @Transactional
    public void sendMessage(String chatId, String userId, MessageRequest messageRequest) {
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(AppError.CHAT_NOT_FOUND));
        ChatMember chatMember = chatMemberRepository
                .findByChat_IdAndMember_Id(chatId, userId)
                .orElseThrow(() -> new AppException(AppError.UNAUTHORIZED));

        if (
                (messageRequest.getType().equals(MessageType.IMAGE)
                        && !S3MediaTypes.isImageType(s3Service
                        .getContentTypeFromS3Url(messageRequest.getContent())))
                || (messageRequest.getType().equals(MessageType.VIDEO)
                        && !S3MediaTypes.isVideoType(s3Service
                        .getContentTypeFromS3Url(messageRequest.getContent())))
                || (messageRequest.getType().equals(MessageType.FILE)
                        && !S3MediaTypes.isAllowedFileType(s3Service
                        .getContentTypeFromS3Url(messageRequest.getContent())))
        )
                throw new AppException(AppError.INVALID_INPUT_DATA);


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
