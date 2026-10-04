package com.govtrack360.config;

import com.govtrack360.entity.*;
import com.govtrack360.repository.*;
import com.govtrack360.service.AuditLogService;
import com.govtrack360.service.NotificationService;
import com.govtrack360.service.PerformanceMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;

/**
 * Seeds Phase 3 demo data: about six months of attendance history (so performance scores are meaningful),
 * a few performance reviews for last month, and some rewards.
 * <p>
 * Runs after {@link DataSeeder}. It works on a fresh database and on an existing Phase 2 database, is a no-op
 * once any review or reward exists, and can be switched off with SEED_PHASE3_DEMO=false.
 * Attendance is only ever added for dates that have no record yet, so real data is never overwritten.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class Phase3DataSeeder implements CommandLineRunner {

    private static final int BACKFILL_DAYS = 200;

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PerformanceReviewRepository reviewRepository;
    private final RewardRepository rewardRepository;
    private final PerformanceMetricsService metricsService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Value("${app.seed.phase3-demo-data:true}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        if (reviewRepository.count() > 0 || rewardRepository.count() > 0) {
            return; // already seeded
        }

        User adminUser = userRepository.findByEmail("admin@govtrack360.com").orElse(null);
        if (adminUser == null) {
            return; // Phase 1/2 demo data is not present, so there is nothing sensible to attach to
        }
        User officerUser = userRepository.findByEmail("officer@govtrack360.com").orElse(adminUser);
        User employeeUser = userRepository.findByEmail("employee@govtrack360.com").orElse(null);

        List<Employee> activeEmployees = employeeRepository.findActive(null);
        if (activeEmployees.isEmpty()) {
            return;
        }

        backfillAttendance(activeEmployees);
        auditLogService.log(null, "SYSTEM_SEED", "ATTENDANCE", "Backfilled attendance history for Phase 3 demo.");

        seedReviews(adminUser, officerUser);
        seedRewards(adminUser, officerUser);

        notificationService.notify(adminUser, "Welcome to GovTrack 360 Phase 3",
                "Explore Performance, Rewards, Reports and Analytics.", NotificationType.SYSTEM);
        if (officerUser != adminUser) {
            notificationService.notify(officerUser, "Welcome to GovTrack 360 Phase 3",
                    "Review and recognise your department's team, and run reports.", NotificationType.SYSTEM);
        }
        if (employeeUser != null) {
            notificationService.notify(employeeUser, "Welcome to GovTrack 360 Phase 3",
                    "See your performance score, reviews and rewards.", NotificationType.SYSTEM);
        }
    }

    // =========================================================================================
    // ATTENDANCE BACKFILL
    // =========================================================================================

    private void backfillAttendance(List<Employee> employees) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(BACKFILL_DAYS);
        LocalDate end = today.minusDays(1);
        List<Long> ids = employees.stream().map(Employee::getId).toList();

        // Existing records, so we never collide with the unique (employee, date) constraint.
        Set<String> existing = new HashSet<>();
        for (Attendance a : attendanceRepository.findForEmployeesBetween(ids, start, end)) {
            existing.add(a.getEmployee().getId() + "|" + a.getDate());
        }
        // Approved leave days are skipped (an employee on leave does not check in).
        Set<String> onLeave = new HashSet<>();
        for (LeaveRequest lr : leaveRequestRepository.findOverlapping(ids, LeaveStatus.APPROVED, start, end)) {
            for (LocalDate d = lr.getStartDate(); !d.isAfter(lr.getEndDate()); d = d.plusDays(1)) {
                onLeave.add(lr.getEmployee().getId() + "|" + d);
            }
        }

        List<Attendance> toSave = new ArrayList<>();
        for (Employee employee : employees) {
            // Give each employee a slightly different reliability so rankings are not all identical.
            int profile = (int) (employee.getId() % 4);
            double absentChance = new double[]{0.02, 0.04, 0.08, 0.12}[profile];
            double lateChance = new double[]{0.05, 0.10, 0.15, 0.25}[profile];

            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
                if (employee.getJoiningDate() != null && d.isBefore(employee.getJoiningDate())) continue;
                String key = employee.getId() + "|" + d;
                if (existing.contains(key) || onLeave.contains(key)) continue;

                Random random = new Random(employee.getId() * 100_000L + d.toEpochDay());
                double roll = random.nextDouble();
                if (roll < absentChance) {
                    continue; // absent: no record
                }

                LocalTime checkIn;
                LocalTime checkOut;
                AttendanceStatus status;
                if (roll < absentChance + lateChance) {
                    status = AttendanceStatus.LATE;
                    checkIn = LocalTime.of(9, 35).plusMinutes(random.nextInt(55));
                    checkOut = LocalTime.of(17, 30).plusMinutes(random.nextInt(60));
                } else if (roll > 0.97) {
                    status = AttendanceStatus.HALF_DAY;
                    checkIn = LocalTime.of(9, random.nextInt(25));
                    checkOut = checkIn.plusHours(3).plusMinutes(random.nextInt(30));
                } else {
                    status = AttendanceStatus.PRESENT;
                    checkIn = LocalTime.of(8, 45).plusMinutes(random.nextInt(40));
                    checkOut = LocalTime.of(17, 0).plusMinutes(random.nextInt(75));
                }

                double hours = Math.round((Duration.between(checkIn, checkOut).toMinutes() / 60.0) * 100.0) / 100.0;
                toSave.add(Attendance.builder()
                        .employee(employee)
                        .date(d)
                        .checkInTime(checkIn)
                        .checkOutTime(checkOut)
                        .totalWorkingHours(hours)
                        .status(status)
                        .build());
            }
        }
        attendanceRepository.saveAll(toSave);
    }

    // =========================================================================================
    // REVIEWS
    // =========================================================================================

    private void seedReviews(User adminUser, User officerUser) {
        YearMonth previous = YearMonth.now().minusMonths(1);
        LocalDate periodStart = previous.atDay(1);
        LocalDate periodEnd = previous.atEndOfMonth();

        createReview("EMP-1003", adminUser, periodStart, periodEnd, 4, 5, 4,
                "Reliable, friendly and always willing to help the team.",
                "Could break larger tasks into smaller milestones.",
                "Strong month overall.");
        createReview("EMP-1004", adminUser, periodStart, periodEnd, 3, 3, 2,
                "Careful with figures and good at documentation.",
                "Reconciliation work needs a second check before submission.",
                "Please focus on first-time-right submissions next month.");
        createReview("EMP-1005", adminUser, periodStart, periodEnd, 4, 4, 5,
                "Proposed a simple site-inspection checklist adopted by the team.",
                "Keep progress notes up to date on tasks.",
                "Great initiative.");
        createReview("EMP-1006", adminUser, periodStart, periodEnd, 5, 4, 4,
                "Delivered the recruitment drive smoothly and on schedule.",
                "Delegate more routine paperwork.",
                "Excellent delivery.");
        createReview("EMP-1007", officerUser, periodStart, periodEnd, 4, 4, 3,
                "Quick at resolving IT support requests.",
                "Document fixes so others can reuse them.",
                "Good steady performance.");
    }

    private void createReview(String employeeCode, User reviewer, LocalDate start, LocalDate end,
                              int quality, int teamwork, int initiative,
                              String strengths, String improvements, String comments) {
        Employee employee = employeeRepository.findByEmployeeId(employeeCode).orElse(null);
        if (employee == null || employee.getStatus() != Status.ACTIVE) {
            return;
        }
        if (employee.getUser() != null && employee.getUser().getId().equals(reviewer.getId())) {
            return; // never self-review
        }

        Double systemScore = metricsService.compute(employee, start, end).getOverallScore();
        double managerScore = PerformanceMetricsService.round1(((quality + teamwork + initiative) / 3.0) * 20.0);
        // Same 70/30 blend as PerformanceService.
        double finalScore = systemScore == null
                ? managerScore
                : PerformanceMetricsService.round1(0.7 * systemScore + 0.3 * managerScore);

        reviewRepository.save(PerformanceReview.builder()
                .employee(employee)
                .reviewer(reviewer)
                .periodStart(start)
                .periodEnd(end)
                .qualityRating(quality)
                .teamworkRating(teamwork)
                .initiativeRating(initiative)
                .systemScore(systemScore)
                .finalScore(finalScore)
                .strengths(strengths)
                .areasForImprovement(improvements)
                .comments(comments)
                .build());

        if (employee.getUser() != null) {
            notificationService.notify(employee.getUser(), "Performance review published",
                    "A performance review for " + start + " to " + end + " has been added. Final score: "
                            + finalScore + ".", NotificationType.SYSTEM);
        }
        auditLogService.log(reviewer, "PERFORMANCE_REVIEW_CREATED", "PERFORMANCE",
                "Seeded review for " + employeeCode + " (" + start + " to " + end + ")");
    }

    // =========================================================================================
    // REWARDS
    // =========================================================================================

    private void seedRewards(User adminUser, User officerUser) {
        LocalDate today = LocalDate.now();
        createReward("EMP-1006", adminUser, RewardType.EMPLOYEE_OF_THE_MONTH, "Employee of the Month",
                "Led the Q3 recruitment drive from start to finish with no missed deadlines.", 50, today.minusDays(20));
        createReward("EMP-1007", officerUser, RewardType.EXCELLENCE_AWARD, "IT Support Excellence",
                "Resolved the highest number of support tickets with consistently positive feedback.", 30, today.minusDays(12));
        createReward("EMP-1003", adminUser, RewardType.TEAM_PLAYER, "Team Player",
                "Stepped in to help colleagues during the onboarding checklist rollout.", 20, today.minusDays(9));
        createReward("EMP-1005", adminUser, RewardType.INNOVATION, "Site Checklist Innovation",
                "Introduced a simple inspection checklist now used across Public Works.", 30, today.minusDays(15));
        createReward("EMP-1004", adminUser, RewardType.APPRECIATION, "Year-end Support",
                "Thank you for the extra hours during budget season.", 10, today.minusDays(30));
        createReward("EMP-1003", adminUser, RewardType.PUNCTUALITY, "Punctuality Award",
                "Perfect on-time record across the month.", 15, today.minusDays(3));
    }

    private void createReward(String employeeCode, User awardedBy, RewardType type, String title,
                              String citation, int points, LocalDate date) {
        Employee employee = employeeRepository.findByEmployeeId(employeeCode).orElse(null);
        if (employee == null || employee.getStatus() != Status.ACTIVE) {
            return;
        }
        if (employee.getUser() != null && employee.getUser().getId().equals(awardedBy.getId())) {
            return; // never self-award
        }

        rewardRepository.save(Reward.builder()
                .employee(employee)
                .awardedBy(awardedBy)
                .type(type)
                .title(title)
                .citation(citation)
                .points(points)
                .awardedDate(date)
                .build());

        if (employee.getUser() != null) {
            notificationService.notify(employee.getUser(), "You received a reward: " + title,
                    type.getLabel() + " (" + points + " points) was awarded to you.", NotificationType.SYSTEM);
        }
        auditLogService.log(awardedBy, "REWARD_GRANTED", "REWARD",
                "Seeded " + type.name() + " (" + points + " pts) for " + employeeCode);
    }
}
