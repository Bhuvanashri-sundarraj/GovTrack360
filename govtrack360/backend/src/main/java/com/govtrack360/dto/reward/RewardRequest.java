package com.govtrack360.dto.reward;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RewardRequest {

    @NotNull(message = "Employee is required")
    private Long employeeId;

    /** One of the RewardType names, e.g. EXCELLENCE_AWARD. */
    @NotBlank(message = "Reward type is required")
    private String type;

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @Size(max = 1000, message = "Citation must be at most 1000 characters")
    private String citation;

    @NotNull(message = "Points are required")
    @Min(value = 1, message = "Points must be between 1 and 100")
    @Max(value = 100, message = "Points must be between 1 and 100")
    private Integer points;

    /** Optional; defaults to today. Cannot be in the future. */
    private LocalDate awardedDate;
}
