package com.govtrack360.dto.task;

import com.govtrack360.dto.common.EmployeeSummary;
import com.govtrack360.dto.common.UserSummary;
import com.govtrack360.dto.department.DepartmentSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String taskCode;
    private String title;
    private String description;
    private DepartmentSummary department;
    private EmployeeSummary assignedTo;
    private UserSummary assignedBy;
    private String priority;
    private String status;
    private LocalDate startDate;
    private LocalDate dueDate;
    private int progressPercentage;
    private boolean overdue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private UserSummary reviewedBy;
    private String reviewComment;
}
