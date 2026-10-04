package com.govtrack360.dto.performance;

import com.govtrack360.dto.common.EmployeeSummary;
import com.govtrack360.dto.department.DepartmentSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * System-calculated performance for one employee over a period. Every rate is a 0-100 percentage and is
 * null when there was nothing to measure (e.g. no tasks fell due in the period).
 */
@Getter
@Builder
@AllArgsConstructor
public class PerformanceScoreResponse {
    private EmployeeSummary employee;
    private DepartmentSummary department;
    private LocalDate from;
    private LocalDate to;

    /** Position in a leaderboard; null outside leaderboard responses or when the employee is not rated. */
    @Setter
    private Integer rank;

    // Task metrics (tasks assigned to the employee whose due date falls in the period)
    private int tasksTotal;
    private int tasksDelivered;
    private int tasksApproved;
    private int tasksOverdue;
    private int rejectionCount;
    private Double completionRate;
    private Double onTimeRate;
    private Double qualityRate;

    // Attendance metrics (Monday-Friday working days, excluding approved leave)
    private int expectedWorkingDays;
    private int presentDays;
    private int lateDays;
    private int halfDays;
    private int leaveDays;
    private Double attendanceRate;
    private Double punctualityRate;

    private Double overallScore;
    private String ratingBand;
}
