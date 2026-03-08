package com.okayji.moderation.listener;

import com.okayji.exception.AppError;
import com.okayji.exception.AppException;
import com.okayji.feed.entity.Post;
import com.okayji.feed.entity.PostMedia;
import com.okayji.feed.entity.PostMediaType;
import com.okayji.feed.entity.PostStatus;
import com.okayji.feed.repository.PostRepository;
import com.okayji.mapper.ModerationMapper;
import com.okayji.moderation.dto.ModerationVerdict;
import com.okayji.moderation.entity.InputType;
import com.okayji.moderation.entity.ModerationDecision;
import com.okayji.moderation.entity.ModerationResult;
import com.okayji.moderation.entity.TargetType;
import com.okayji.moderation.event.PostModerationEvent;
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

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "POST-MODERATION-LISTENER")
public class PostModerationListener {

    private final PostRepository postRepository;
    private final ModerationResultRepository moderationResultRepository;
    private final ModerationService moderationService;
    private final ModerationMapper moderationMapper;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(PostModerationEvent event) {
        log.info("Received Post Moderation Event with post id={}", event.getSource());
        String postId = (String) event.getSource();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(AppError.POST_NOT_FOUND));
        boolean reject = false;

        // TEXT
        if (post.getContent() != null && !post.getContent().isBlank()) {
            ModerationVerdict verdict = moderationService.moderateText(post.getContent());
            ModerationResult result = moderationMapper.toModerationResult(
                    verdict, TargetType.POST, post.getId(), InputType.TEXT
            );
            moderationResultRepository.save(result);

            if (verdict.decision().equals(ModerationDecision.BLOCK)) {
                reject = true;
            }
        }

        // MEDIA
        for (PostMedia m : post.getPostMedia()) {
            if (reject) break;

            if (m.getType() == PostMediaType.IMAGE) {
                ModerationVerdict verdict = moderationService.moderateImageUrl(m.getMediaUrl());
                ModerationResult result = moderationMapper.toModerationResult(
                        verdict, TargetType.POST, post.getId(), InputType.IMAGE
                );
                moderationResultRepository.save(result);

                if (verdict.decision().equals(ModerationDecision.BLOCK))
                    reject = true;
            }
            else if (m.getType() == PostMediaType.VIDEO) {
                List<ModerationVerdict> verdicts = moderationService.moderateVideoUrl(m.getMediaUrl());

                for (ModerationVerdict verdict : verdicts) {
                    moderationResultRepository.save(moderationMapper.toModerationResult(
                            verdict, TargetType.POST, post.getId(), InputType.VIDEO_FRAME)
                    );
                    if (verdict.decision().equals(ModerationDecision.BLOCK)) {
                        reject = true;
                        break;
                    }
                }
            }
        }

        PostStatus newStatus = reject ? PostStatus.REJECTED : decideFromDb(postId);
        log.info("Moderated post id={}, new post status={}", event.getSource(), newStatus);
        setStatusAndSaveToDb(post, newStatus);

        if (newStatus == PostStatus.REJECTED || newStatus == PostStatus.UNDER_REVIEW)
            notificationService.sendNotification(
                    NotificationFactory.violatedPost(post.getUser(), postId, newStatus)
            );
    }

    private PostStatus decideFromDb(String postId) {
        List<ModerationResult> moderationResults = moderationResultRepository.findByTargetId(postId);
        boolean review = false;

        for (ModerationResult m :  moderationResults) {
            if (m.getDecision().equals(ModerationDecision.BLOCK))
                return PostStatus.REJECTED;
            if (m.getDecision().equals(ModerationDecision.REVIEW))
                review = true;
        }
        return review ? PostStatus.UNDER_REVIEW : PostStatus.PUBLISHED;
    }

    private void setStatusAndSaveToDb(Post post, PostStatus status) {
        post.setStatus(status);
        postRepository.save(post);
    }
}
