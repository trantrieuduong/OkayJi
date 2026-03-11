package com.okayji.moderation.service.impl;

import com.okayji.feed.entity.*;
import com.okayji.feed.repository.CommentRepository;
import com.okayji.feed.repository.PostRepository;
import com.okayji.mapper.ModerationMapper;
import com.okayji.moderation.dto.ModerationVerdict;
import com.okayji.moderation.entity.*;
import com.okayji.moderation.repository.ModerationResultRepository;
import com.okayji.moderation.service.ModerationOrchestrator;
import com.okayji.moderation.service.ModerationService;
import com.okayji.notification.service.NotificationFactory;
import com.okayji.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j(topic = "MODERATION-ORCHESTRATOR")
@RequiredArgsConstructor
public class ModerationOrchestratorImpl implements ModerationOrchestrator {
    private final PostRepository postRepository;
    private final ModerationService moderationService;
    private final ModerationMapper moderationMapper;
    private final ModerationResultRepository moderationResultRepository;
    private final NotificationService notificationService;
    private final CommentRepository commentRepository;

    @Override
    public void processPost(ModerationJob job) {
        String postId = job.getTargetId();
        log.info("Received Post Moderation with post id={}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
        boolean reject = false;

        // TEXT
        if (post.getContent() != null && !post.getContent().isBlank()) {
            ModerationVerdict verdict = moderationService.moderateText(post.getContent());
            ModerationResult result = moderationMapper.toModerationResult(
                    verdict, job
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
                        verdict, job
                );
                moderationResultRepository.save(result);

                if (verdict.decision().equals(ModerationDecision.BLOCK))
                    reject = true;
            }
            else if (m.getType() == PostMediaType.VIDEO) {
                List<ModerationVerdict> verdicts = moderationService.moderateVideoUrl(m.getMediaUrl());

                for (ModerationVerdict verdict : verdicts) {
                    moderationResultRepository.save(
                            moderationMapper.toModerationResult(verdict, job)
                    );
                    if (verdict.decision().equals(ModerationDecision.BLOCK)) {
                        reject = true;
                        break;
                    }
                }
            }
        }

        PostStatus newStatus = reject ? PostStatus.REJECTED : decideFromDb(job);
        log.info("Moderated post id={}, new post status={}", postId, newStatus);
        post.setStatus(newStatus);
        postRepository.save(post);

        if (newStatus == PostStatus.REJECTED || newStatus == PostStatus.UNDER_REVIEW)
            notificationService.sendNotification(
                    NotificationFactory.violatedPost(post.getUser(), postId, newStatus)
            );
    }

    @Override
    public void processComment(ModerationJob job) {
        String commentId = job.getTargetId();
        log.info("Received Comment Moderation with comment id={}", commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        if (comment.getContent() != null && !comment.getContent().isBlank()) {
            ModerationVerdict verdict = moderationService.moderateText(comment.getContent());
            ModerationResult result = moderationMapper.toModerationResult(
                    verdict, job
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

    private PostStatus decideFromDb(ModerationJob job) {
        List<ModerationResult> moderationResults = job.getModerationResults();
        boolean review = false;

        for (ModerationResult m :  moderationResults) {
            if (m.getDecision().equals(ModerationDecision.BLOCK))
                return PostStatus.REJECTED;
            if (m.getDecision().equals(ModerationDecision.REVIEW))
                review = true;
        }
        return review ? PostStatus.UNDER_REVIEW : PostStatus.PUBLISHED;
    }
}
