package com.okayji.moderation.service;

import com.okayji.moderation.entity.ModerationJob;

public interface ModerationOrchestrator {
    void processPost(ModerationJob job);
    void processComment(ModerationJob job);
}
