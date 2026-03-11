package com.okayji.moderation.repository;

import com.okayji.moderation.entity.ModerationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModerationResultRepository extends JpaRepository<ModerationResult,Long> {
    List<ModerationResult> findByModerationJobId(Long moderationJobId);
}
