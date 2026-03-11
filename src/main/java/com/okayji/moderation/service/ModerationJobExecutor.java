package com.okayji.moderation.service;

import com.okayji.feed.entity.Post;
import com.okayji.feed.entity.PostStatus;
import com.okayji.feed.repository.PostRepository;
import com.okayji.moderation.entity.ModerationJob;
import com.okayji.moderation.entity.ModerationJobStatus;
import com.okayji.moderation.entity.TargetType;
import com.okayji.moderation.repository.ModerationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "MODERATION-JOB-EXECUTOR")
public class ModerationJobExecutor {

    private final ModerationJobRepository moderationJobRepository;
    private final PostRepository postRepository;
    private final ModerationOrchestrator moderationOrchestrator;

    @Transactional
    public void prepareAndExecute(Long jobId) throws Exception {
        ModerationJob job = moderationJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Moderation job not found: " + jobId));

        if (job.getStatus() == ModerationJobStatus.DONE
                || job.getStatus() == ModerationJobStatus.FAILED) {
            return;
        }

        job.setStatus(ModerationJobStatus.PROCESSING);
        job.setRetryCount(job.getRetryCount() + 1);
        moderationJobRepository.save(job);

        execute(jobId);
    }

    @Retryable(
            retryFor = {
                    org.springframework.web.client.ResourceAccessException.class,
                    java.net.SocketTimeoutException.class,
                    java.io.IOException.class,
                    java.net.ConnectException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    @Transactional
    public void execute(Long jobId) throws Exception {
        ModerationJob job = moderationJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Moderation job not found: " + jobId));

        switch (job.getTargetType()) {
            case POST -> moderationOrchestrator.processPost(job);
            case COMMENT -> moderationOrchestrator.processComment(job);
        }

        job.setStatus(ModerationJobStatus.DONE);
        moderationJobRepository.save(job);
    }

    @Recover
    @Transactional
    public void recover(Exception ex, Long jobId) {
        ModerationJob job = moderationJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Moderation job not found: " + jobId));

        log.error("Moderation job {} failed after retries", jobId, ex);

        if (job.getRetryCount() < job.getMaxRetries()) {
            job.setStatus(ModerationJobStatus.PENDING);
        } else {
            job.setStatus(ModerationJobStatus.FAILED);
            markTargetNeedsReview(job);
        }

        moderationJobRepository.save(job);
    }

    private void markTargetNeedsReview(ModerationJob job) {
        if (Objects.requireNonNull(job.getTargetType()) == TargetType.POST) {
            Post post = postRepository.findById(job.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("Post not found: " + job.getTargetId()));
            post.setStatus(PostStatus.UNDER_REVIEW);
            postRepository.save(post);
        }
    }
}
