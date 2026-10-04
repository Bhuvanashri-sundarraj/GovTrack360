package com.govtrack360.dto.performance;

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
public class PerformanceReviewResponse {
    private Long id;
    private EmployeeSummary employee;
    private DepartmentSummary department;
    private UserSummary reviewer;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private int qualityRating;
    private int teamworkRating;
    private int initiativeRating;
    /** Average of the three ratings scaled to 0-100. */
    private Double managerScore;
    private Double systemScore;
    private Double finalScore;
    private String ratingBand;
    private String strengths;
    private String areasForImprovement;
    private String comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
