package com.okayji.moderation.listener;

import com.okayji.exception.AppError;
import com.okayji.exception.AppException;
import com.okayji.feed.entity.Comment;
import com.okayji.feed.repository.CommentRepository;
import com.okayji.mapper.ModerationMapper;
import com.okayji.moderation.dto.ModerationVerdict;
import com.okayji.moderation.entity.InputType;
import com.okayji.moderation.entity.ModerationDecision;
import com.okayji.moderation.entity.ModerationResult;
import com.okayji.moderation.entity.TargetType;
import com.okayji.moderation.event.CommentModerationEvent;
import com.okayji.moderation.repository.ModerationResultRepository;
import com.okayji.moderation.service.ModerationService;
import com.okayji.notification.service.NotificationFactory;
import com.okayji.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "COMMENT-MODERATION-LISTENER")
public class CommentModerationListener {

    private final CommentRepository commentRepository;
    private final ModerationResultRepository moderationResultRepository;
    private final ModerationService moderationService;
    private final ModerationMapper moderationMapper;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(CommentModerationEvent event) {
        log.info("Received Comment Moderation Event with comment id={}", event.getSource());
        String commentId = (String) event.getSource();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(AppError.COMMENT_NOT_FOUND));

        if (comment.getContent() != null && !comment.getContent().isBlank()) {
            log.info("Moderating text comment id={}", event.getSource());
            ModerationVerdict verdict = moderationService.moderateText(comment.getContent());
            ModerationResult result = moderationMapper.toModerationResult(
                    verdict, TargetType.COMMENT, commentId, InputType.TEXT
            );
            moderationResultRepository.save(result);

            if (verdict.decision().equals(ModerationDecision.BLOCK)) { // Delete and send noti if violated
                notificationService.sendNotification(
                        NotificationFactory.violatedComment(comment.getUser(), comment.getPost().getId())
                );
                commentRepository.delete(comment);
            }
        }
    }
}
