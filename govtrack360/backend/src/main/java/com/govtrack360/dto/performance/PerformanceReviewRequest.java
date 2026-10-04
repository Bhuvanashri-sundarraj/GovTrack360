package com.govtrack360.dto.performance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PerformanceReviewRequest {

    @NotNull(message = "Employee is required")
    private Long employeeId;

    @NotNull(message = "Period start date is required")
    private LocalDate periodStart;

    @NotNull(message = "Period end date is required")
    private LocalDate periodEnd;

    @NotNull(message = "Quality rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer qualityRating;

    @NotNull(message = "Teamwork rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer teamworkRating;

    @NotNull(message = "Initiative rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer initiativeRating;

    @Size(max = 1000, message = "Strengths must be at most 1000 characters")
    private String strengths;

    @Size(max = 1000, message = "Areas for improvement must be at most 1000 characters")
    private String areasForImprovement;

    @Size(max = 1000, message = "Comments must be at most 1000 characters")
    private String comments;
}
