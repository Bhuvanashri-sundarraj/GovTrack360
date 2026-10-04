package com.govtrack360.dto.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskStatusChangeRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String comment;
}
