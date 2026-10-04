package com.govtrack360.dto.task;

import com.govtrack360.dto.common.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class TaskCommentResponse {
    private Long id;
    private UserSummary author;
    private String comment;
    private LocalDateTime createdAt;
}
