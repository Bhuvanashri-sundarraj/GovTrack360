package com.govtrack360.service;

import com.govtrack360.dto.performance.*;
import com.govtrack360.entity.*;
import com.govtrack360.exception.DuplicateResourceException;
import com.govtrack360.exception.InvalidRequestException;
import com.govtrack360.exception.ResourceNotFoundException;
import com.govtrack360.repository.EmployeeRepository;
import com.govtrack360.repository.PerformanceReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Performance scores, leaderboards, trends and formal reviews.
 * <p>
 * Final review score = 70% system score (objective, from tasks and attendance) + 30% manager score
 * (average of the three 1-5 ratings scaled to 0-100). If the system had no data for the period, the
 * final score is simply the manager score.
 */
@Service
@RequiredArgsConstructor
public class PerformanceService {

    private static final double SYSTEM_WEIGHT = 0.7;
    private static final double MANAGER_WEIGHT = 0.3;
    private static final int MAX_TREND_MONTHS = 24;

    private final PerformanceMetricsService metricsService;
    private final AccessScopeService scopeService;
    private final PerformanceReviewRepository reviewRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final SummaryMapper mapper;

    // =========================================================================================
    // SCORES
    // =========================================================================================

    @Transactional(readOnly = true)
    public PerformanceScoreResponse getMyScore(User actor, LocalDate from, LocalDate to) {
        Employee employee = scopeService.requireOwnEmployee(actor);
        DateRange range = DateRange.resolve(from, to);
        return metricsService.compute(employee, range.from(), range.to());
    }

    @Transactional(readOnly = true)
    public PerformanceScoreResponse getEmployeeScore(Long employeeId, User actor, LocalDate from, LocalDate to) {
        Employee employee = findEmployee(employeeId);
        scopeService.assertCanAccessEmployee(actor, employee);
        DateRange range = DateRange.resolve(from, to);
        return metricsService.compute(employee, range.from(), range.to());
    }

    @Transactional(readOnly = true)
    public List<PerformanceTrendPoint> getMyTrend(User actor, Integer months) {
        Employee employee = scopeService.requireOwnEmployee(actor);
        return metricsService.trend(employee, resolveMonths(months));
    }

    @Transactional(readOnly = true)
    public List<PerformanceTrendPoint> getEmployeeTrend(Long employeeId, User actor, Integer months) {
        Employee employee = findEmployee(employeeId);
        scopeService.assertCanAccessEmployee(actor, employee);
        return metricsService.trend(employee, resolveMonths(months));
    }

    /**
     * Ranked scores for every active employee the actor may see (ADMIN: everyone or one department,
     * OFFICER: own department only).
     */
    @Transactional(readOnly = true)
    public List<PerformanceScoreResponse> getLeaderboard(User actor, Long departmentId, LocalDate from, LocalDate to) {
        DateRange range = DateRange.resolve(from, to);
        List<Employee> employees = scopeService.activeEmployeesInScope(actor, departmentId);
        return metricsService.rank(metricsService.computeBulk(employees, range.from(), range.to()));
    }

    @Transactional(readOnly = true)
    public ScopeResponse getScope(User actor) {
        if (actor.getRole() == Role.OFFICER) {
            Employee own = scopeService.findOwnEmployee(actor).orElse(null);
            if (own != null) {
                return ScopeResponse.builder()
                        .restricted(true)
                        .departmentId(own.getDepartment().getId())
                        .departmentName(own.getDepartment().getDepartmentName())
                        .build();
            }
        }
        return ScopeResponse.builder().restricted(false).build();
    }

    /**
     * Active employees the caller may review or recognise: everyone (ADMIN) or their own department
     * (OFFICER), always excluding the caller.
     */
    @Transactional(readOnly = true)
    public List<EligibleEmployeeResponse> getEligibleEmployees(User actor) {
        Employee self = scopeService.findOwnEmployee(actor).orElse(null);
        return scopeService.activeEmployeesInScope(actor, null).stream()
                .filter(e -> self == null || !e.getId().equals(self.getId()))
                .sorted(Comparator.comparing(Employee::getFirstName).thenComparing(Employee::getLastName))
                .map(e -> EligibleEmployeeResponse.builder()
                        .id(e.getId())
                        .employeeId(e.getEmployeeId())
                        .fullName(mapper.fullName(e))
                        .designation(e.getDesignation())
                        .departmentId(e.getDepartment().getId())
                        .departmentName(e.getDepartment().getDepartmentName())
                        .build())
                .toList();
    }

    // =========================================================================================
    // REVIEWS
    // =========================================================================================

    @Transactional
    public PerformanceReviewResponse createReview(PerformanceReviewRequest request, User actor) {
        Employee employee = findEmployee(request.getEmployeeId());
        assertCanReview(actor, employee);

        if (employee.getStatus() != Status.ACTIVE) {
            throw new InvalidRequestException("Reviews can only be written for active employees.");
        }
        validatePeriod(request.getPeriodStart(), request.getPeriodEnd());

        if (reviewRepository.existsByEmployeeAndPeriodStartAndPeriodEnd(
                employee, request.getPeriodStart(), request.getPeriodEnd())) {
            throw new DuplicateResourceException(
                    "A review already exists for this employee and period. Edit the existing review instead.");
        }

        PerformanceReview review = PerformanceReview.builder()
                .employee(employee)
                .reviewer(actor)
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .qualityRating(request.getQualityRating())
                .teamworkRating(request.getTeamworkRating())
                .initiativeRating(request.getInitiativeRating())
                .strengths(clean(request.getStrengths()))
                .areasForImprovement(clean(request.getAreasForImprovement()))
                .comments(clean(request.getComments()))
                .build();
        applyScores(review);
        review = reviewRepository.save(review);

        if (employee.getUser() != null) {
            notificationService.notify(employee.getUser(), "Performance review published",
                    "A performance review for " + review.getPeriodStart() + " to " + review.getPeriodEnd()
                            + " has been added. Final score: " + review.getFinalScore() + ".",
                    NotificationType.SYSTEM);
        }
        auditLogService.log(actor, "PERFORMANCE_REVIEW_CREATED", "PERFORMANCE",
                "Reviewed " + employee.getEmployeeId() + " for " + review.getPeriodStart() + " to " + review.getPeriodEnd());

        return toResponse(review);
    }

    @Transactional
    public PerformanceReviewResponse updateReview(Long id, PerformanceReviewUpdateRequest request, User actor) {
        PerformanceReview review = findReview(id);
        assertCanReview(actor, review.getEmployee());

        // An officer may only edit reviews they wrote; an admin may edit any.
        if (actor.getRole() == Role.OFFICER && !review.getReviewer().getId().equals(actor.getId())) {
            throw new AccessDeniedException("You can only edit reviews that you wrote.");
        }

        review.setQualityRating(request.getQualityRating());
        review.setTeamworkRating(request.getTeamworkRating());
        review.setInitiativeRating(request.getInitiativeRating());
        review.setStrengths(clean(request.getStrengths()));
        review.setAreasForImprovement(clean(request.getAreasForImprovement()));
        review.setComments(clean(request.getComments()));
        applyScores(review);
        review = reviewRepository.save(review);

        auditLogService.log(actor, "PERFORMANCE_REVIEW_UPDATED", "PERFORMANCE",
                "Updated review #" + review.getId() + " for " + review.getEmployee().getEmployeeId());

        return toResponse(review);
    }

    @Transactional
    public void deleteReview(Long id, User actor) {
        PerformanceReview review = findReview(id);
        String code = review.getEmployee().getEmployeeId();
        reviewRepository.delete(review);
        auditLogService.log(actor, "PERFORMANCE_REVIEW_DELETED", "PERFORMANCE",
                "Deleted review #" + id + " for " + code);
    }

    @Transactional(readOnly = true)
    public PerformanceReviewResponse getReview(Long id, User actor) {
        PerformanceReview review = findReview(id);
        scopeService.assertCanAccessEmployee(actor, review.getEmployee());
        return toResponse(review);
    }

    /**
     * ADMIN: all reviews (optionally narrowed). OFFICER: reviews of their own department only.
     */
    @Transactional(readOnly = true)
    public List<PerformanceReviewResponse> searchReviews(User actor, Long employeeId, Long departmentId) {
        Long scopedDepartmentId = scopeService.resolveDepartmentScope(actor, departmentId);
        List<PerformanceReview> reviews = (scopedDepartmentId == null)
                ? reviewRepository.findAllOrdered()
                : reviewRepository.findByDepartmentId(scopedDepartmentId);

        return reviews.stream()
                .filter(r -> employeeId == null || r.getEmployee().getId().equals(employeeId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PerformanceReviewResponse> getMyReviews(User actor) {
        Employee employee = scopeService.requireOwnEmployee(actor);
        return reviewRepository.findByEmployeeId(employee.getId()).stream().map(this::toResponse).toList();
    }

    // =========================================================================================
    // INTERNAL
    // =========================================================================================

    private void assertCanReview(User actor, Employee employee) {
        // Nobody reviews themselves, regardless of role.
        Employee own = scopeService.findOwnEmployee(actor).orElse(null);
        if (own != null && own.getId().equals(employee.getId())) {
            throw new AccessDeniedException("You cannot review your own performance.");
        }
        scopeService.assertCanAccessEmployee(actor, employee);
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new InvalidRequestException("The review period start cannot be after its end.");
        }
        if (end.isAfter(LocalDate.now())) {
            throw new InvalidRequestException("The review period cannot end in the future.");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > DateRange.MAX_DAYS) {
            throw new InvalidRequestException("The review period cannot exceed " + DateRange.MAX_DAYS + " days.");
        }
    }

    /**
     * Snapshots the system score for the review's period and derives manager and final scores.
     */
    private void applyScores(PerformanceReview review) {
        PerformanceScoreResponse system = metricsService.compute(
                review.getEmployee(), review.getPeriodStart(), review.getPeriodEnd());
        Double systemScore = system.getOverallScore();
        double managerScore = managerScore(review);

        review.setSystemScore(systemScore);
        review.setFinalScore(systemScore == null
                ? managerScore
                : PerformanceMetricsService.round1(SYSTEM_WEIGHT * systemScore + MANAGER_WEIGHT * managerScore));
    }

    private double managerScore(PerformanceReview review) {
        double average = (review.getQualityRating() + review.getTeamworkRating() + review.getInitiativeRating()) / 3.0;
        return PerformanceMetricsService.round1(average * 20.0);
    }

    private int resolveMonths(Integer months) {
        int value = (months == null) ? 6 : months;
        if (value < 1 || value > MAX_TREND_MONTHS) {
            throw new InvalidRequestException("Months must be between 1 and " + MAX_TREND_MONTHS + ".");
        }
        return value;
    }

    private String clean(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    private PerformanceReview findReview(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Performance review not found with id: " + id));
    }

    private PerformanceReviewResponse toResponse(PerformanceReview review) {
        Employee employee = review.getEmployee();
        return PerformanceReviewResponse.builder()
                .id(review.getId())
                .employee(mapper.employee(employee))
                .department(mapper.department(employee.getDepartment()))
                .reviewer(mapper.user(review.getReviewer()))
                .periodStart(review.getPeriodStart())
                .periodEnd(review.getPeriodEnd())
                .qualityRating(review.getQualityRating())
                .teamworkRating(review.getTeamworkRating())
                .initiativeRating(review.getInitiativeRating())
                .managerScore(managerScore(review))
                .systemScore(review.getSystemScore())
                .finalScore(review.getFinalScore())
                .ratingBand(PerformanceMetricsService.bandFor(review.getFinalScore()))
                .strengths(review.getStrengths())
                .areasForImprovement(review.getAreasForImprovement())
                .comments(review.getComments())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
