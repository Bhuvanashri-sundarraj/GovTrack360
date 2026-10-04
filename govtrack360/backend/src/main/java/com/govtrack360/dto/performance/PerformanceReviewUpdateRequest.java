package com.govtrack360.dto.performance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Employee and period are fixed once a review exists, so an update only carries ratings and text.
 */
@Getter
@Setter
public class PerformanceReviewUpdateRequest {

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
