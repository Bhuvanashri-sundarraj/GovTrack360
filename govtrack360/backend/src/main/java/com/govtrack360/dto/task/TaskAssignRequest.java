package com.govtrack360.dto.task;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskAssignRequest {

    @NotNull(message = "Employee is required")
    private Long employeeId;

    private String comment;
}
