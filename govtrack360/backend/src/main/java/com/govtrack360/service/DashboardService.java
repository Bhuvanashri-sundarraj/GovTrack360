package com.govtrack360.service;

import com.govtrack360.dto.attendance.TodayAttendanceSummaryResponse;
import com.govtrack360.dto.dashboard.AdminDashboardResponse;
import com.govtrack360.dto.dashboard.EmployeeDashboardResponse;
import com.govtrack360.dto.dashboard.OfficerDashboardResponse;
import com.govtrack360.dto.department.DepartmentResponse;
import com.govtrack360.dto.employee.EmployeeResponse;
import com.govtrack360.dto.performance.PerformanceScoreResponse;
import com.govtrack360.entity.*;
import com.govtrack360.exception.ResourceNotFoundException;
import com.govtrack360.repository.AttendanceRepository;
import com.govtrack360.repository.DepartmentRepository;
import com.govtrack360.repository.EmployeeRepository;
import com.govtrack360.repository.LeaveRequestRepository;
import com.govtrack360.repository.RewardRepository;
import com.govtrack360.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<TaskStatus> TERMINAL_STATUSES = List.of(TaskStatus.APPROVED, TaskStatus.CANCELLED);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentService departmentService;
    private final EmployeeService employeeService;
    private final TaskRepository taskRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceService attendanceService;
    private final PerformanceMetricsService performanceMetricsService;
    private final RewardRepository rewardRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        long totalEmployees = employeeRepository.count();
        long activeEmployees = employeeRepository.countByStatus(Status.ACTIVE);
        long totalDepartments = departmentRepository.count();
        long activeDepartments = departmentRepository.countByStatus(Status.ACTIVE);

        List<EmployeeResponse> recentEmployees = employeeRepository.findTop5ByOrderByCreatedAtDesc()
                .stream().map(e -> employeeService.getById(e.getId())).toList();

        List<DepartmentResponse> departmentOverview = departmentService.getAll(null);

        // Phase 2 stats
        long activeTasks = taskRepository.countByStatus(TaskStatus.ASSIGNED) + taskRepository.countByStatus(TaskStatus.IN_PROGRESS);
        long tasksAwaitingReview = taskRepository.countByStatus(TaskStatus.SUBMITTED) + taskRepository.countByStatus(TaskStatus.UNDER_REVIEW);
        long overdueTasks = taskRepository.countOverdue(LocalDate.now(), TERMINAL_STATUSES);
        TodayAttendanceSummaryResponse todaySummary = attendanceService.getTodaySummary();
        long pendingLeaveRequests = leaveRequestRepository.countByStatus(LeaveStatus.PENDING);

        List<String> recentActivity = List.of(
                totalEmployees + " employee record(s) currently in the system.",
                totalDepartments + " department(s) configured.",
                activeTasks + " task(s) currently active across all departments.",
                pendingLeaveRequests + " leave request(s) awaiting a decision."
        );

        return AdminDashboardResponse.builder()
                .totalEmployees(totalEmployees)
                .activeEmployees(activeEmployees)
                .totalDepartments(totalDepartments)
                .activeDepartments(activeDepartments)
                .recentEmployees(recentEmployees)
                .departmentOverview(departmentOverview)
                .recentActivity(recentActivity)
                .activeTasks(activeTasks)
                .tasksAwaitingReview(tasksAwaitingReview)
                .overdueTasks(overdueTasks)
                .presentToday(todaySummary.getPresentToday())
                .pendingLeaveRequests(pendingLeaveRequests)
                .averagePerformanceScore(averageScore(
                        employeeRepository.findActive(null)))
                .rewardsThisMonth(rewardsThisMonth(null))
                .build();
    }

    @Transactional(readOnly = true)
    public OfficerDashboardResponse getOfficerDashboard(User officer) {
        long totalEmployees = employeeRepository.count();
        long activeEmployees = employeeRepository.countByStatus(Status.ACTIVE);
        long totalDepartments = departmentRepository.count();

        List<EmployeeResponse> recentEmployees = employeeRepository.findTop5ByOrderByCreatedAtDesc()
                .stream().map(e -> employeeService.getById(e.getId())).toList();

        List<DepartmentResponse> departments = departmentService.getAll(null);

        // Phase 2 stats are department-scoped to mirror TaskService/LeaveService's own
        // enforcement, so these numbers always match what this officer is actually allowed to see.
        long assignedTasks = 0;
        long tasksAwaitingReview = 0;
        long overdueTasks = 0;
        long pendingLeaveRequests = 0;
        Double averagePerformanceScore = null;
        long rewardsThisMonth = 0;

        Employee officerEmployee = employeeRepository.findByUser(officer).orElse(null);
        if (officerEmployee != null) {
            Long departmentId = officerEmployee.getDepartment().getId();
            assignedTasks = taskRepository.countByDepartmentIdAndStatus(departmentId, TaskStatus.ASSIGNED)
                    + taskRepository.countByDepartmentIdAndStatus(departmentId, TaskStatus.IN_PROGRESS);
            tasksAwaitingReview = taskRepository.countByDepartmentIdAndStatus(departmentId, TaskStatus.SUBMITTED)
                    + taskRepository.countByDepartmentIdAndStatus(departmentId, TaskStatus.UNDER_REVIEW);
            overdueTasks = taskRepository.countOverdueByDepartment(departmentId, LocalDate.now(), TERMINAL_STATUSES);
            pendingLeaveRequests = leaveRequestRepository.search(null, departmentId, LeaveStatus.PENDING).size();
            averagePerformanceScore = averageScore(employeeRepository.findActive(departmentId));
            rewardsThisMonth = rewardsThisMonth(departmentId);
        }

        return OfficerDashboardResponse.builder()
                .totalEmployees(totalEmployees)
                .activeEmployees(activeEmployees)
                .totalDepartments(totalDepartments)
                .recentEmployees(recentEmployees)
                .departments(departments)
                .assignedTasks(assignedTasks)
                .tasksAwaitingReview(tasksAwaitingReview)
                .overdueTasks(overdueTasks)
                .pendingLeaveRequests(pendingLeaveRequests)
                .averagePerformanceScore(averagePerformanceScore)
                .rewardsThisMonth(rewardsThisMonth)
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeDashboardResponse getEmployeeDashboard(User user) {
        Employee employee = employeeRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No employee record is linked to this account yet. Contact an administrator."));

        // Phase 2 stats
        long myActiveTasks = taskRepository.countByAssignedToAndStatus(employee, TaskStatus.ASSIGNED)
                + taskRepository.countByAssignedToAndStatus(employee, TaskStatus.IN_PROGRESS);

        LocalDate today = LocalDate.now();
        long tasksDueSoon = taskRepository.findByAssignedToAndDueDateBetweenAndStatusNotIn(
                employee, today, today.plusDays(3), TERMINAL_STATUSES).size();

        long tasksAwaitingReview = taskRepository.countByAssignedToAndStatus(employee, TaskStatus.SUBMITTED)
                + taskRepository.countByAssignedToAndStatus(employee, TaskStatus.UNDER_REVIEW);

        String todayAttendanceStatus = attendanceRepository.findByEmployeeAndDate(employee, today)
                .map(a -> a.getStatus().name())
                .orElse("NOT_CHECKED_IN");

        long pendingLeaveRequests = leaveRequestRepository.findByEmployeeOrderByAppliedAtDesc(employee)
                .stream().filter(lr -> lr.getStatus() == LeaveStatus.PENDING).count();

        DateRange scoreRange = DateRange.resolve(null, null);
        PerformanceScoreResponse myScore = performanceMetricsService.compute(employee, scoreRange.from(), scoreRange.to());
        Long rewardPoints = rewardRepository.sumPointsByEmployeeId(employee.getId());

        return EmployeeDashboardResponse.builder()
                .fullName(employee.getFirstName() + " " + employee.getLastName())
                .employeeId(employee.getEmployeeId())
                .designation(employee.getDesignation())
                .departmentName(employee.getDepartment().getDepartmentName())
                .status(employee.getStatus().name())
                .email(employee.getEmail())
                .joiningDate(employee.getJoiningDate().toString())
                .upcomingModules(List.of()) // Phase 3 modules are now live, nothing left to preview
                .myActiveTasks(myActiveTasks)
                .tasksDueSoon(tasksDueSoon)
                .tasksAwaitingReview(tasksAwaitingReview)
                .todayAttendanceStatus(todayAttendanceStatus)
                .pendingLeaveRequests(pendingLeaveRequests)
                .performanceScore(myScore.getOverallScore())
                .performanceBand(myScore.getRatingBand())
                .rewardPoints(rewardPoints != null ? rewardPoints : 0L)
                .rewardCount(rewardRepository.findByEmployeeId(employee.getId()).size())
                .build();
    }

    // =========================================================================================
    // Phase 3 helpers
    // =========================================================================================

    /** Average overall score over the default rolling 90-day window; null if nobody could be rated. */
    private Double averageScore(List<Employee> employees) {
        DateRange range = DateRange.resolve(null, null);
        double sum = 0;
        int count = 0;
        for (PerformanceScoreResponse score : performanceMetricsService.computeBulk(employees, range.from(), range.to())) {
            if (score.getOverallScore() != null) {
                sum += score.getOverallScore();
                count++;
            }
        }
        return count > 0 ? PerformanceMetricsService.round1(sum / count) : null;
    }

    private long rewardsThisMonth(Long departmentId) {
        YearMonth ym = YearMonth.now();
        return (departmentId == null)
                ? rewardRepository.findByAwardedDateBetween(ym.atDay(1), ym.atEndOfMonth()).size()
                : rewardRepository.findByDepartmentIdAndAwardedDateBetween(departmentId, ym.atDay(1), ym.atEndOfMonth()).size();
    }
}
