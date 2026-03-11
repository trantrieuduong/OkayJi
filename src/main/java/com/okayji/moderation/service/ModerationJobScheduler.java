package com.okayji.moderation.service;

import com.okayji.moderation.entity.ModerationJob;
import com.okayji.moderation.entity.ModerationJobStatus;
import com.okayji.moderation.repository.ModerationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "MODERATION-JOB-SCHEDULER")
public class ModerationJobScheduler {
    private final ModerationJobRepository moderationJobRepository;
    private final ModerationJobExecutor moderationJobExecutor;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void requeueInterruptedJobsOnStartup() {
        int updated = moderationJobRepository.requeueProcessingJobs(
                ModerationJobStatus.PROCESSING,
                ModerationJobStatus.PENDING
        );
        log.info("Requeued {} interrupted moderation jobs", updated);
    }

    @Scheduled(fixedDelayString = "${app.moderation.scheduler.delay-ms:30000}")
    @Transactional
    public void processPendingJobs() {
        List<ModerationJob> jobs = moderationJobRepository.findJobsForProcessing(
                ModerationJobStatus.PENDING,
                PageRequest.of(0, 10)
        );

        for (ModerationJob job : jobs) {
            try {
                moderationJobExecutor.prepareAndExecute(job.getId());
            } catch (Exception ex) {
                log.error("Cannot submit moderation job {}", job.getId(), ex);
            }
        }
    }
}
