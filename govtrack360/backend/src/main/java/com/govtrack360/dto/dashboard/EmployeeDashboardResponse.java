package com.govtrack360.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class EmployeeDashboardResponse {
    private String fullName;
    private String employeeId;
    private String designation;
    private String departmentName;
    private String status;
    private String email;
    private String joiningDate;
    private List<String> upcomingModules;

    // Phase 2 additions
    private long myActiveTasks;
    private long tasksDueSoon;
    private long tasksAwaitingReview;
    private String todayAttendanceStatus;
    private long pendingLeaveRequests;

    // Phase 3 additions
    private Double performanceScore;
    private String performanceBand;
    private long rewardPoints;
    private long rewardCount;
}
