package com.govtrack360.dto.task;

import com.govtrack360.entity.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Department is required")
    private Long departmentId;

    /**
     * Optional. If provided, the task is created directly in ASSIGNED status.
     * If omitted, the task is created as PENDING and can be assigned later.
     */
    private Long assignedToEmployeeId;

    @NotNull(message = "Priority is required")
    private TaskPriority priority;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;
}
