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
public class OfficerDashboardResponse {
    private long totalEmployees;
    private long activeEmployees;
    private long totalDepartments;
    private List<EmployeeResponse> recentEmployees;
    private List<DepartmentResponse> departments;

    // Phase 2 additions
    private long assignedTasks;
    private long tasksAwaitingReview;
    private long overdueTasks;
    private long pendingLeaveRequests;

    // Phase 3 additions (department-scoped)
    private Double averagePerformanceScore;
    private long rewardsThisMonth;
}
