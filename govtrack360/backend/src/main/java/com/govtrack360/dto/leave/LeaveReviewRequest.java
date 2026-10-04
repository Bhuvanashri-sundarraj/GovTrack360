package com.govtrack360.dto.leave;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveReviewRequest {

    /**
     * "APPROVE" or "REJECT"
     */
    @NotBlank(message = "Decision is required")
    private String decision;

    private String comment;
}
