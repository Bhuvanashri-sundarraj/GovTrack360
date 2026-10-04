package com.govtrack360.dto.dashboard;

import com.govtrack360.dto.department.DepartmentResponse;
import com.govtrack360.dto.employee.EmployeeResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AdminDashboardResponse {
    private long totalEmployees;
    private long activeEmployees;
    private long totalDepartments;
    private long activeDepartments;
    private List<EmployeeResponse> recentEmployees;
    private List<DepartmentResponse> departmentOverview;
    private List<String> recentActivity;

    // Phase 2 additions
    private long activeTasks;
    private long tasksAwaitingReview;
    private long overdueTasks;
    private long presentToday;
    private long pendingLeaveRequests;

    // Phase 3 additions
    private Double averagePerformanceScore;
    private long rewardsThisMonth;
}
