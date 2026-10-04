package com.govtrack360.service;

import com.govtrack360.dto.analytics.AnalyticsOverviewResponse;
import com.govtrack360.dto.analytics.AnalyticsOverviewResponse.*;
import com.govtrack360.dto.performance.PerformanceScoreResponse;
import com.govtrack360.entity.*;
import com.govtrack360.exception.ResourceNotFoundException;
import com.govtrack360.repository.DepartmentRepository;
import com.govtrack360.repository.RewardRepository;
import com.govtrack360.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Aggregated analytics for ADMIN (whole organisation or one department) and OFFICER (own department).
 * Everything is calculated from live data on each request.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int TREND_MONTHS = 6;
    private static final int LIST_SIZE = 5;
    private static final double ATTENTION_THRESHOLD = 65.0;

    private final AccessScopeService scopeService;
    private final PerformanceMetricsService metricsService;
    private final TaskRepository taskRepository;
    private final RewardRepository rewardRepository;
    private final DepartmentRepository departmentRepository;
    private final RewardService rewardService;

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse getOverview(User actor, Long departmentId, LocalDate from, LocalDate to) {
        DateRange range = DateRange.resolve(from, to);
        Long scopedDepartmentId = scopeService.resolveDepartmentScope(actor, departmentId);

        String scope = "All Departments";
        if (scopedDepartmentId != null) {
            scope = departmentRepository.findById(scopedDepartmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + scopedDepartmentId))
                    .getDepartmentName();
        }

        List<Employee> employees = scopeService.activeEmployeesInScope(actor, scopedDepartmentId);
        List<PerformanceScoreResponse> scores =
                metricsService.rank(metricsService.computeBulk(employees, range.from(), range.to()));

        List<Reward> rewards = (scopedDepartmentId == null)
                ? rewardRepository.findByAwardedDateBetween(range.from(), range.to())
                : rewardRepository.findByDepartmentIdAndAwardedDateBetween(scopedDepartmentId, range.from(), range.to());

        List<Task> tasks = (scopedDepartmentId == null)
                ? taskRepository.findByDueDateRange(range.from(), range.to())
                : taskRepository.findByDepartmentAndDueDateRange(scopedDepartmentId, range.from(), range.to());

        return AnalyticsOverviewResponse.builder()
                .from(range.from())
                .to(range.to())
                .scope(scope)
                .kpis(buildKpis(scores, rewards))
                .departments(buildDepartmentStats(scores, rewards))
                .taskStatusDistribution(buildTaskStatusSlices(tasks))
                .taskPriorityDistribution(buildTaskPrioritySlices(tasks))
                .ratingBandDistribution(buildBandSlices(scores))
                .topPerformers(buildTopPerformers(scores))
                .needsAttention(buildNeedsAttention(scores))
                .scoreTrend(buildTrend(employees))
                .recentRewards(rewards.stream().limit(LIST_SIZE).map(rewardService::toResponse).toList())
                .build();
    }

    // =========================================================================================
    // SECTIONS
    // =========================================================================================

    private Kpis buildKpis(List<PerformanceScoreResponse> scores, List<Reward> rewards) {
        int tasksTotal = scores.stream().mapToInt(PerformanceScoreResponse::getTasksTotal).sum();
        int tasksDelivered = scores.stream().mapToInt(PerformanceScoreResponse::getTasksDelivered).sum();
        int overdue = scores.stream().mapToInt(PerformanceScoreResponse::getTasksOverdue).sum();

        return Kpis.builder()
                .employeesEvaluated(scores.size())
                .averageScore(average(scores.stream().map(PerformanceScoreResponse::getOverallScore).toList()))
                .taskCompletionRate(tasksTotal > 0 ? PerformanceMetricsService.round1(tasksDelivered * 100.0 / tasksTotal) : null)
                .onTimeRate(average(scores.stream().map(PerformanceScoreResponse::getOnTimeRate).toList()))
                .attendanceRate(average(scores.stream().map(PerformanceScoreResponse::getAttendanceRate).toList()))
                .tasksInPeriod(tasksTotal)
                .overdueTasks(overdue)
                .rewardsGiven(rewards.size())
                .rewardPoints(rewards.stream().mapToLong(Reward::getPoints).sum())
                .build();
    }

    private List<DepartmentStat> buildDepartmentStats(List<PerformanceScoreResponse> scores, List<Reward> rewards) {
        Map<Long, List<PerformanceScoreResponse>> byDept = new LinkedHashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (PerformanceScoreResponse s : scores) {
            Long id = s.getDepartment().getId();
            byDept.computeIfAbsent(id, k -> new ArrayList<>()).add(s);
            names.put(id, s.getDepartment().getDepartmentName());
        }
        Map<Long, Long> pointsByDept = new HashMap<>();
        for (Reward r : rewards) {
            pointsByDept.merge(r.getEmployee().getDepartment().getId(), (long) r.getPoints(), Long::sum);
        }

        List<DepartmentStat> stats = new ArrayList<>();
        for (Map.Entry<Long, List<PerformanceScoreResponse>> entry : byDept.entrySet()) {
            List<PerformanceScoreResponse> group = entry.getValue();
            int total = group.stream().mapToInt(PerformanceScoreResponse::getTasksTotal).sum();
            int delivered = group.stream().mapToInt(PerformanceScoreResponse::getTasksDelivered).sum();
            stats.add(DepartmentStat.builder()
                    .departmentId(entry.getKey())
                    .departmentName(names.get(entry.getKey()))
                    .employees(group.size())
                    .averageScore(average(group.stream().map(PerformanceScoreResponse::getOverallScore).toList()))
                    .completionRate(total > 0 ? PerformanceMetricsService.round1(delivered * 100.0 / total) : null)
                    .attendanceRate(average(group.stream().map(PerformanceScoreResponse::getAttendanceRate).toList()))
                    .rewardPoints(pointsByDept.getOrDefault(entry.getKey(), 0L))
                    .build());
        }
        stats.sort(Comparator.comparing(DepartmentStat::getDepartmentName));
        return stats;
    }

    private List<Slice> buildTaskStatusSlices(List<Task> tasks) {
        Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
        for (Task task : tasks) {
            counts.merge(task.getStatus(), 1L, Long::sum);
        }
        List<Slice> slices = new ArrayList<>();
        for (TaskStatus status : TaskStatus.values()) {
            long count = counts.getOrDefault(status, 0L);
            if (count > 0) {
                slices.add(Slice.builder().key(status.name()).label(ReportService.label(status.name())).value(count).build());
            }
        }
        return slices;
    }

    private List<Slice> buildTaskPrioritySlices(List<Task> tasks) {
        Map<TaskPriority, Long> counts = new EnumMap<>(TaskPriority.class);
        for (Task task : tasks) {
            counts.merge(task.getPriority(), 1L, Long::sum);
        }
        List<Slice> slices = new ArrayList<>();
        for (TaskPriority priority : TaskPriority.values()) {
            long count = counts.getOrDefault(priority, 0L);
            if (count > 0) {
                slices.add(Slice.builder().key(priority.name()).label(ReportService.label(priority.name())).value(count).build());
            }
        }
        return slices;
    }

    private List<Slice> buildBandSlices(List<PerformanceScoreResponse> scores) {
        List<String> order = List.of("OUTSTANDING", "VERY_GOOD", "GOOD", "NEEDS_IMPROVEMENT", "UNSATISFACTORY", "NOT_RATED");
        Map<String, Long> counts = new HashMap<>();
        for (PerformanceScoreResponse s : scores) {
            counts.merge(s.getRatingBand(), 1L, Long::sum);
        }
        List<Slice> slices = new ArrayList<>();
        for (String band : order) {
            long count = counts.getOrDefault(band, 0L);
            if (count > 0) {
                slices.add(Slice.builder().key(band).label(ReportService.label(band)).value(count).build());
            }
        }
        return slices;
    }

    private List<RankedEmployee> buildTopPerformers(List<PerformanceScoreResponse> ranked) {
        return ranked.stream()
                .filter(s -> s.getOverallScore() != null)
                .limit(LIST_SIZE)
                .map(this::toRanked)
                .toList();
    }

    private List<RankedEmployee> buildNeedsAttention(List<PerformanceScoreResponse> ranked) {
        List<PerformanceScoreResponse> low = new ArrayList<>(ranked.stream()
                .filter(s -> s.getOverallScore() != null && s.getOverallScore() < ATTENTION_THRESHOLD)
                .toList());
        low.sort(Comparator.comparing(PerformanceScoreResponse::getOverallScore));
        return low.stream().limit(LIST_SIZE).map(this::toRanked).toList();
    }

    private RankedEmployee toRanked(PerformanceScoreResponse s) {
        return RankedEmployee.builder()
                .rank(s.getRank() != null ? s.getRank() : 0)
                .employeeId(s.getEmployee().getId())
                .employeeCode(s.getEmployee().getEmployeeId())
                .fullName(s.getEmployee().getFullName())
                .departmentName(s.getDepartment().getDepartmentName())
                .score(s.getOverallScore())
                .ratingBand(s.getRatingBand())
                .build();
    }

    private List<TrendPoint> buildTrend(List<Employee> employees) {
        List<TrendPoint> points = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            List<PerformanceScoreResponse> monthScores = metricsService.computeBulk(employees, ym.atDay(1), ym.atEndOfMonth());
            int total = monthScores.stream().mapToInt(PerformanceScoreResponse::getTasksTotal).sum();
            int delivered = monthScores.stream().mapToInt(PerformanceScoreResponse::getTasksDelivered).sum();
            points.add(TrendPoint.builder()
                    .label(PerformanceMetricsService.monthLabel(ym))
                    .averageScore(average(monthScores.stream().map(PerformanceScoreResponse::getOverallScore).toList()))
                    .completionRate(total > 0 ? PerformanceMetricsService.round1(delivered * 100.0 / total) : null)
                    .attendanceRate(average(monthScores.stream().map(PerformanceScoreResponse::getAttendanceRate).toList()))
                    .build());
        }
        return points;
    }

    private Double average(List<Double> values) {
        double sum = 0;
        int count = 0;
        for (Double v : values) {
            if (v != null) {
                sum += v;
                count++;
            }
        }
        return count > 0 ? PerformanceMetricsService.round1(sum / count) : null;
    }
}
