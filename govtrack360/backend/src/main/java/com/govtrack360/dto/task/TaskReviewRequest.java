package com.govtrack360.dto.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskReviewRequest {

    /**
     * "APPROVE" or "REJECT"
     */
    @NotBlank(message = "Decision is required")
    private String decision;

    private String comment;
}
