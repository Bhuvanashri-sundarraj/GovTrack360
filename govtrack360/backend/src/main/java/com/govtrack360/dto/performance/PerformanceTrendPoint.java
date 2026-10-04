package com.govtrack360.dto.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PerformanceTrendPoint {
    /** e.g. "Sep 2026" */
    private String label;
    private int month;
    private int year;
    private Double overallScore;
    private Double completionRate;
    private Double attendanceRate;
}
