package com.okayji.moderation.repository;

import com.okayji.moderation.entity.ModerationJob;
import com.okayji.moderation.entity.ModerationJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ModerationJobRepository extends JpaRepository<ModerationJob,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select j
        from ModerationJob j
        where j.status = :status and j.retryCount < j.maxRetries
        order by j.createdAt asc
    """)
    List<ModerationJob> findJobsForProcessing(@Param("status") ModerationJobStatus status, Pageable pageable);

    @Modifying
    @Query("""
        update ModerationJob j
        set j.status = :pending
        where j.status = :processing
    """)
    int requeueProcessingJobs(@Param("processing") ModerationJobStatus processing,
                              @Param("pending") ModerationJobStatus pending);
}
