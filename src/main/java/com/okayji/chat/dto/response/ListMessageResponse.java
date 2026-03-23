package com.okayji.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ListMessageResponse(List<MessageResponse> items,
                                  Long nextCursorSeq) {
}
