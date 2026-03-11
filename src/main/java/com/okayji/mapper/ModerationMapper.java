package com.okayji.mapper;

import com.okayji.moderation.dto.ModerationVerdict;
import com.okayji.moderation.entity.ModerationJob;
import com.okayji.moderation.entity.ModerationResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ModerationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "moderationJob.id", source = "job.id")
    ModerationResult toModerationResult(ModerationVerdict moderationVerdict,
                                        ModerationJob job);
}
