package com.govtrack360.dto.task;

import com.govtrack360.dto.common.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class TaskWorkflowHistoryResponse {
    private Long id;
    private String action;
    private String previousStatus;
    private String newStatus;
    private UserSummary performedBy;
    private String comment;
    private LocalDateTime timestamp;
}
