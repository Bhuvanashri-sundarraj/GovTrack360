package com.govtrack360.service;

import com.govtrack360.dto.performance.PerformanceScoreResponse;
import com.govtrack360.dto.performance.PerformanceTrendPoint;
import com.govtrack360.entity.*;
import com.govtrack360.repository.AttendanceRepository;
import com.govtrack360.repository.LeaveRequestRepository;
import com.govtrack360.repository.TaskRepository;
import com.govtrack360.repository.TaskWorkflowHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calculates objective performance metrics straight from live Phase 2 data (tasks, workflow history,
 * attendance, leave). Nothing here is stored; the numbers are always current.
 *
 * <h3>Definitions</h3>
 * <ul>
 *   <li><b>Tasks in scope</b>: tasks assigned to the employee, not cancelled, whose due date falls inside the
 *       period. Only days up to today are evaluated, so tasks that are not due yet never count against anyone.</li>
 *   <li><b>Delivered</b>: APPROVED, SUBMITTED or UNDER_REVIEW (the employee has done their part).</li>
 *   <li><b>Completion rate</b>: delivered / tasks in scope.</li>
 *   <li><b>On-time rate</b>: delivered tasks submitted on or before the due date, divided by
 *       (delivered + tasks still open past their due date).</li>
 *   <li><b>Quality rate</b>: approvals / (approvals + rejections) across review decisions on those tasks.</li>
 *   <li><b>Attendance rate</b>: (present + late + half x 0.5) / expected working days. Working days are
 *       Monday-Friday from the later of the period start and the joining date, excluding approved leave.</li>
 *   <li><b>Punctuality rate</b>: attended days that were not LATE / attended days.</li>
 * </ul>
 * The overall score is the weighted average of whichever rates could be measured (weights are renormalised
 * when a rate has no data, so an employee with no tasks is judged on attendance alone and vice versa).
 */
@Service
@RequiredArgsConstructor
public class PerformanceMetricsService {

    static final double W_COMPLETION = 25;
    static final double W_ON_TIME = 20;
    static final double W_QUALITY = 15;
    static final double W_ATTENDANCE = 25;
    static final double W_PUNCTUALITY = 15;

    private static final Set<TaskStatus> DELIVERED =
            EnumSet.of(TaskStatus.APPROVED, TaskStatus.SUBMITTED, TaskStatus.UNDER_REVIEW);

    private final TaskRepository taskRepository;
    private final TaskWorkflowHistoryRepository workflowHistoryRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final SummaryMapper mapper;

    // =========================================================================================
    // PUBLIC API
    // =========================================================================================

    @Transactional(readOnly = true)
    public PerformanceScoreResponse compute(Employee employee, LocalDate from, LocalDate to) {
        return computeBulk(List.of(employee), from, to).get(0);
    }

    /**
     * Computes scores for many employees using a fixed number of queries (not one set per employee).
     * The result list is in the same order as the input list.
     */
    @Transactional(readOnly = true)
    public List<PerformanceScoreResponse> computeBulk(List<Employee> employees, LocalDate from, LocalDate to) {
        if (employees.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        LocalDate evalTo = to.isAfter(today) ? today : to;
        boolean emptyWindow = evalTo.isBefore(from);

        List<Long> ids = employees.stream().map(Employee::getId).toList();

        List<Task> tasks = emptyWindow
                ? List.of()
                : taskRepository.findForPerformance(ids, from, evalTo, TaskStatus.CANCELLED);
        Map<Long, List<Task>> tasksByEmployee = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getAssignedTo().getId()));

        Map<Long, Long> rejectionsByTask = new HashMap<>();
        if (!tasks.isEmpty()) {
            List<Long> taskIds = tasks.stream().map(Task::getId).toList();
            for (Object[] row : workflowHistoryRepository.countByTaskIdsAndAction(taskIds, "TASK_REJECTED")) {
                rejectionsByTask.put((Long) row[0], (Long) row[1]);
            }
        }

        List<Attendance> attendance = emptyWindow
                ? List.of()
                : attendanceRepository.findForEmployeesBetween(ids, from, evalTo);
        Map<Long, List<Attendance>> attendanceByEmployee = attendance.stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId()));

        List<LeaveRequest> leaves = emptyWindow
                ? List.of()
                : leaveRequestRepository.findOverlapping(ids, LeaveStatus.APPROVED, from, evalTo);
        Map<Long, List<LeaveRequest>> leavesByEmployee = leaves.stream()
                .collect(Collectors.groupingBy(l -> l.getEmployee().getId()));

        List<PerformanceScoreResponse> results = new ArrayList<>(employees.size());
        for (Employee employee : employees) {
            results.add(buildScore(employee, from, evalTo, today,
                    tasksByEmployee.getOrDefault(employee.getId(), List.of()),
                    rejectionsByTask,
                    attendanceByEmployee.getOrDefault(employee.getId(), List.of()),
                    leavesByEmployee.getOrDefault(employee.getId(), List.of())));
        }
        return results;
    }

    /**
     * Assigns competition ranks (1, 2, 2, 4...) by overall score, highest first. Unrated employees are
     * placed last and get no rank. Returns a new sorted list; the input is not modified.
     */
    public List<PerformanceScoreResponse> rank(List<PerformanceScoreResponse> scores) {
        List<PerformanceScoreResponse> sorted = new ArrayList<>(scores);
        sorted.sort(Comparator
                .comparing((PerformanceScoreResponse s) -> s.getOverallScore() == null ? 1 : 0)
                .thenComparing(s -> s.getOverallScore() == null ? 0.0 : -s.getOverallScore())
                .thenComparing(s -> s.getEmployee().getFullName()));

        int position = 0;
        Double previousScore = null;
        int previousRank = 0;
        for (PerformanceScoreResponse s : sorted) {
            if (s.getOverallScore() == null) {
                s.setRank(null);
                continue;
            }
            position++;
            if (previousScore != null && previousScore.equals(s.getOverallScore())) {
                s.setRank(previousRank);
            } else {
                s.setRank(position);
                previousRank = position;
                previousScore = s.getOverallScore();
            }
        }
        return sorted;
    }

    /**
     * Month-by-month scores for one employee, oldest first, ending with the current month.
     */
    @Transactional(readOnly = true)
    public List<PerformanceTrendPoint> trend(Employee employee, int months) {
        List<PerformanceTrendPoint> points = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            PerformanceScoreResponse score = compute(employee, ym.atDay(1), ym.atEndOfMonth());
            points.add(PerformanceTrendPoint.builder()
                    .label(monthLabel(ym))
                    .month(ym.getMonthValue())
                    .year(ym.getYear())
                    .overallScore(score.getOverallScore())
                    .completionRate(score.getCompletionRate())
                    .attendanceRate(score.getAttendanceRate())
                    .build());
        }
        return points;
    }

    public static String monthLabel(YearMonth ym) {
        return ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + ym.getYear();
    }

    public static String bandFor(Double score) {
        if (score == null) {
            return "NOT_RATED";
        }
        if (score >= 90) return "OUTSTANDING";
        if (score >= 80) return "VERY_GOOD";
        if (score >= 65) return "GOOD";
        if (score >= 50) return "NEEDS_IMPROVEMENT";
        return "UNSATISFACTORY";
    }

    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    // =========================================================================================
    // INTERNALS
    // =========================================================================================

    private PerformanceScoreResponse buildScore(Employee employee, LocalDate from, LocalDate evalTo, LocalDate today,
                                                List<Task> tasks, Map<Long, Long> rejectionsByTask,
                                                List<Attendance> attendance, List<LeaveRequest> leaves) {
        // ---- Task metrics ----
        int total = tasks.size();
        int delivered = 0;
        int approved = 0;
        int onTimeDelivered = 0;
        int overdueOpen = 0;
        long rejections = 0;

        for (Task task : tasks) {
            rejections += rejectionsByTask.getOrDefault(task.getId(), 0L);
            if (DELIVERED.contains(task.getStatus())) {
                delivered++;
                if (task.getStatus() == TaskStatus.APPROVED) {
                    approved++;
                }
                if (wasOnTime(task)) {
                    onTimeDelivered++;
                }
            } else if (task.getDueDate().isBefore(today)) {
                overdueOpen++;
            }
        }

        Double completionRate = total > 0 ? round1(delivered * 100.0 / total) : null;
        int onTimeDenominator = delivered + overdueOpen;
        Double onTimeRate = onTimeDenominator > 0 ? round1(onTimeDelivered * 100.0 / onTimeDenominator) : null;
        long qualityDenominator = approved + rejections;
        Double qualityRate = qualityDenominator > 0 ? round1(approved * 100.0 / qualityDenominator) : null;

        // ---- Attendance metrics ----
        LocalDate effectiveStart = employee.getJoiningDate() != null && employee.getJoiningDate().isAfter(from)
                ? employee.getJoiningDate() : from;

        Set<LocalDate> leaveDays = new HashSet<>();
        for (LeaveRequest leave : leaves) {
            LocalDate start = leave.getStartDate().isBefore(effectiveStart) ? effectiveStart : leave.getStartDate();
            LocalDate end = leave.getEndDate().isAfter(evalTo) ? evalTo : leave.getEndDate();
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                if (isWorkingDay(d)) {
                    leaveDays.add(d);
                }
            }
        }

        int present = 0;
        int late = 0;
        int half = 0;
        for (Attendance record : attendance) {
            LocalDate d = record.getDate();
            if (d.isBefore(effectiveStart) || d.isAfter(evalTo) || !isWorkingDay(d)) {
                continue;
            }
            switch (record.getStatus()) {
                case PRESENT -> present++;
                case LATE -> late++;
                case HALF_DAY -> half++;
                case ON_LEAVE -> leaveDays.add(d);
                default -> { /* ABSENT rows (if ever stored) simply count as not attended */ }
            }
        }

        int expected = 0;
        for (LocalDate d = effectiveStart; !d.isAfter(evalTo); d = d.plusDays(1)) {
            if (isWorkingDay(d) && !leaveDays.contains(d)) {
                expected++;
            }
        }

        double attendedWeighted = present + late + half * 0.5;
        Double attendanceRate = expected > 0 ? round1(Math.min(100.0, attendedWeighted * 100.0 / expected)) : null;
        int attendedDays = present + late + half;
        Double punctualityRate = attendedDays > 0 ? round1((present + half) * 100.0 / attendedDays) : null;

        // ---- Overall ----
        double weighted = 0;
        double weightSum = 0;
        if (completionRate != null) { weighted += completionRate * W_COMPLETION; weightSum += W_COMPLETION; }
        if (onTimeRate != null) { weighted += onTimeRate * W_ON_TIME; weightSum += W_ON_TIME; }
        if (qualityRate != null) { weighted += qualityRate * W_QUALITY; weightSum += W_QUALITY; }
        if (attendanceRate != null) { weighted += attendanceRate * W_ATTENDANCE; weightSum += W_ATTENDANCE; }
        if (punctualityRate != null) { weighted += punctualityRate * W_PUNCTUALITY; weightSum += W_PUNCTUALITY; }
        Double overall = weightSum > 0 ? round1(weighted / weightSum) : null;

        return PerformanceScoreResponse.builder()
                .employee(mapper.employee(employee))
                .department(mapper.department(employee.getDepartment()))
                .from(from)
                .to(evalTo)
                .tasksTotal(total)
                .tasksDelivered(delivered)
                .tasksApproved(approved)
                .tasksOverdue(overdueOpen)
                .rejectionCount((int) rejections)
                .completionRate(completionRate)
                .onTimeRate(onTimeRate)
                .qualityRate(qualityRate)
                .expectedWorkingDays(expected)
                .presentDays(present)
                .lateDays(late)
                .halfDays(half)
                .leaveDays(leaveDays.size())
                .attendanceRate(attendanceRate)
                .punctualityRate(punctualityRate)
                .overallScore(overall)
                .ratingBand(bandFor(overall))
                .build();
    }

    /**
     * A delivered task is on time when it was submitted on or before its due date. If, for any reason, no
     * submission timestamp exists, the task is given the benefit of the doubt.
     */
    private boolean wasOnTime(Task task) {
        if (task.getSubmittedAt() == null) {
            return true;
        }
        return !task.getSubmittedAt().toLocalDate().isAfter(task.getDueDate());
    }

    private boolean isWorkingDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }
}
