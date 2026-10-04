package com.govtrack360.service;

import com.govtrack360.dto.performance.PerformanceScoreResponse;
import com.govtrack360.dto.report.*;
import com.govtrack360.entity.*;
import com.govtrack360.exception.InvalidRequestException;
import com.govtrack360.exception.ResourceNotFoundException;
import com.govtrack360.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Generates the Phase 3 reports. All reports share one tabular shape (columns + rows + summary) so the UI
 * and the CSV exporter handle them uniformly.
 * <p>
 * Scoping: ADMIN may report on everything or on one department; OFFICER is always limited to their own
 * department. Tasks are placed in a period by their due date.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final AccessScopeService scopeService;
    private final PerformanceMetricsService metricsService;
    private final TaskRepository taskRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final RewardRepository rewardRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogService auditLogService;
    private final SummaryMapper mapper;

    // =========================================================================================
    // CATALOGUE
    // =========================================================================================

    public List<ReportTypeInfo> getTypes() {
        return Arrays.stream(ReportType.values())
                .map(t -> ReportTypeInfo.builder()
                        .value(t.name())
                        .label(t.getLabel())
                        .description(t.getDescription())
                        .build())
                .toList();
    }

    // =========================================================================================
    // GENERATE
    // =========================================================================================

    @Transactional(readOnly = true)
    public ReportResponse generate(ReportType type, Long departmentId, LocalDate from, LocalDate to, User actor) {
        DateRange range = DateRange.resolve(from, to);
        Long scopedDepartmentId = scopeService.resolveDepartmentScope(actor, departmentId);

        String scope = "All Departments";
        if (scopedDepartmentId != null) {
            scope = departmentRepository.findById(scopedDepartmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + scopedDepartmentId))
                    .getDepartmentName();
        }

        ReportBuilder builder = new ReportBuilder();
        switch (type) {
            case TASK_SUMMARY -> buildTaskSummary(builder, scopedDepartmentId, range);
            case EMPLOYEE_PERFORMANCE -> buildEmployeePerformance(builder, actor, scopedDepartmentId, range);
            case ATTENDANCE -> buildAttendance(builder, actor, scopedDepartmentId, range);
            case LEAVE -> buildLeave(builder, scopedDepartmentId, range);
            case DEPARTMENT_SUMMARY -> buildDepartmentSummary(builder, actor, scopedDepartmentId, range);
            case REWARDS -> buildRewards(builder, scopedDepartmentId, range);
        }

        return ReportResponse.builder()
                .type(type.name())
                .title(type.getLabel() + " Report")
                .description(type.getDescription())
                .from(range.from())
                .to(range.to())
                .scope(scope)
                .generatedBy(actor.getEmail())
                .generatedAt(LocalDateTime.now())
                .summary(builder.summary)
                .columns(builder.columns)
                .rows(builder.rows)
                .build();
    }

    // =========================================================================================
    // CSV EXPORT
    // =========================================================================================

    @Transactional
    public byte[] exportCsv(ReportType type, Long departmentId, LocalDate from, LocalDate to, User actor) {
        ReportResponse report = generate(type, departmentId, from, to, actor);

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF'); // BOM so Excel opens UTF-8 correctly
        csv.append(joinCsv(report.getColumns().stream().map(ReportColumn::getLabel).toList())).append("\r\n");
        for (Map<String, Object> row : report.getRows()) {
            List<Object> values = new ArrayList<>();
            for (ReportColumn column : report.getColumns()) {
                values.add(row.get(column.getKey()));
            }
            csv.append(joinCsv(values)).append("\r\n");
        }

        auditLogService.log(actor, "REPORT_EXPORTED", "REPORT",
                "Exported " + type.name() + " report (" + report.getFrom() + " to " + report.getTo()
                        + ", " + report.getScope() + ", " + report.getRows().size() + " rows)");

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static String csvFileName(ReportType type, LocalDate from, LocalDate to) {
        return "govtrack360-" + type.name().toLowerCase().replace('_', '-') + "-" + from + "-to-" + to + ".csv";
    }

    /**
     * Quotes fields as needed and neutralises spreadsheet formula injection: text that starts with
     * = + - @ is prefixed with an apostrophe so Excel/Sheets never evaluates it. Real numbers are untouched.
     */
    static String joinCsv(List<?> values) {
        StringJoiner joiner = new StringJoiner(",");
        for (Object value : values) {
            joiner.add(csvField(value));
        }
        return joiner.toString();
    }

    static String csvField(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (!(value instanceof Number) && !text.isEmpty()) {
            char first = text.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
                text = "'" + text;
            }
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            text = "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    // =========================================================================================
    // REPORT BUILDERS
    // =========================================================================================

    private void buildTaskSummary(ReportBuilder b, Long departmentId, DateRange range) {
        List<Task> tasks = (departmentId == null)
                ? taskRepository.findByDueDateRange(range.from(), range.to())
                : taskRepository.findByDepartmentAndDueDateRange(departmentId, range.from(), range.to());

        b.column("taskCode", "Task Code", "text");
        b.column("title", "Title", "text");
        b.column("department", "Department", "text");
        b.column("assignee", "Assigned To", "text");
        b.column("priority", "Priority", "text");
        b.column("status", "Status", "text");
        b.column("progress", "Progress (%)", "number");
        b.column("dueDate", "Due Date", "text");
        b.column("overdue", "Overdue", "text");

        LocalDate today = LocalDate.now();
        int approved = 0, active = 0, review = 0, overdue = 0, cancelled = 0;
        for (Task task : tasks) {
            boolean isOverdue = task.getDueDate().isBefore(today)
                    && task.getStatus() != TaskStatus.APPROVED && task.getStatus() != TaskStatus.CANCELLED;
            switch (task.getStatus()) {
                case APPROVED -> approved++;
                case ASSIGNED, IN_PROGRESS -> active++;
                case SUBMITTED, UNDER_REVIEW -> review++;
                case CANCELLED -> cancelled++;
                default -> { }
            }
            if (isOverdue) overdue++;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("taskCode", task.getTaskCode());
            row.put("title", task.getTitle());
            row.put("department", task.getDepartment().getDepartmentName());
            row.put("assignee", task.getAssignedTo() != null ? mapper.fullName(task.getAssignedTo()) : "Unassigned");
            row.put("priority", task.getPriority().name());
            row.put("status", task.getStatus().name());
            row.put("progress", task.getProgressPercentage());
            row.put("dueDate", task.getDueDate().toString());
            row.put("overdue", isOverdue ? "Yes" : "No");
            b.rows.add(row);
        }

        int considered = tasks.size() - cancelled;
        b.summary("Total tasks", String.valueOf(tasks.size()));
        b.summary("Approved", String.valueOf(approved));
        b.summary("Active", String.valueOf(active));
        b.summary("Awaiting review", String.valueOf(review));
        b.summary("Overdue", String.valueOf(overdue));
        b.summary("Approval rate", considered > 0
                ? PerformanceMetricsService.round1(approved * 100.0 / considered) + "%" : "n/a");
    }

    private void buildEmployeePerformance(ReportBuilder b, User actor, Long departmentId, DateRange range) {
        List<Employee> employees = scopeService.activeEmployeesInScope(actor, departmentId);
        List<PerformanceScoreResponse> scores =
                metricsService.rank(metricsService.computeBulk(employees, range.from(), range.to()));

        b.column("rank", "Rank", "number");
        b.column("employeeId", "Employee ID", "text");
        b.column("name", "Name", "text");
        b.column("department", "Department", "text");
        b.column("designation", "Designation", "text");
        b.column("tasks", "Tasks", "number");
        b.column("delivered", "Delivered", "number");
        b.column("completion", "Completion (%)", "number");
        b.column("onTime", "On-time (%)", "number");
        b.column("quality", "Quality (%)", "number");
        b.column("attendance", "Attendance (%)", "number");
        b.column("punctuality", "Punctuality (%)", "number");
        b.column("score", "Overall Score", "number");
        b.column("band", "Rating", "text");

        double total = 0;
        int rated = 0;
        int needsImprovement = 0;
        String top = null;
        for (PerformanceScoreResponse s : scores) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", s.getRank());
            row.put("employeeId", s.getEmployee().getEmployeeId());
            row.put("name", s.getEmployee().getFullName());
            row.put("department", s.getDepartment().getDepartmentName());
            row.put("designation", s.getEmployee().getDesignation());
            row.put("tasks", s.getTasksTotal());
            row.put("delivered", s.getTasksDelivered());
            row.put("completion", s.getCompletionRate());
            row.put("onTime", s.getOnTimeRate());
            row.put("quality", s.getQualityRate());
            row.put("attendance", s.getAttendanceRate());
            row.put("punctuality", s.getPunctualityRate());
            row.put("score", s.getOverallScore());
            row.put("band", label(s.getRatingBand()));
            b.rows.add(row);

            if (s.getOverallScore() != null) {
                total += s.getOverallScore();
                rated++;
                if (top == null) top = s.getEmployee().getFullName();
                if (s.getOverallScore() < 65) needsImprovement++;
            }
        }
        b.summary("Employees", String.valueOf(scores.size()));
        b.summary("Rated", String.valueOf(rated));
        b.summary("Average score", rated > 0 ? String.valueOf(PerformanceMetricsService.round1(total / rated)) : "n/a");
        b.summary("Top performer", top != null ? top : "n/a");
        b.summary("Below 65", String.valueOf(needsImprovement));
    }

    private void buildAttendance(ReportBuilder b, User actor, Long departmentId, DateRange range) {
        List<Employee> employees = scopeService.activeEmployeesInScope(actor, departmentId);
        // Copy into a mutable list: computeBulk may return an immutable empty list.
        List<PerformanceScoreResponse> scores =
                new ArrayList<>(metricsService.computeBulk(employees, range.from(), range.to()));

        b.column("employeeId", "Employee ID", "text");
        b.column("name", "Name", "text");
        b.column("department", "Department", "text");
        b.column("expected", "Working Days", "number");
        b.column("present", "Present", "number");
        b.column("late", "Late", "number");
        b.column("half", "Half Days", "number");
        b.column("leave", "Leave Days", "number");
        b.column("absent", "Absent", "number");
        b.column("attendance", "Attendance (%)", "number");

        scores.sort(Comparator.comparing(s -> s.getEmployee().getFullName()));
        int totalExpected = 0, totalAttended = 0, totalLate = 0;
        double rateSum = 0;
        int rateCount = 0;
        for (PerformanceScoreResponse s : scores) {
            int attended = s.getPresentDays() + s.getLateDays() + s.getHalfDays();
            int absent = Math.max(0, s.getExpectedWorkingDays() - attended);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("employeeId", s.getEmployee().getEmployeeId());
            row.put("name", s.getEmployee().getFullName());
            row.put("department", s.getDepartment().getDepartmentName());
            row.put("expected", s.getExpectedWorkingDays());
            row.put("present", s.getPresentDays());
            row.put("late", s.getLateDays());
            row.put("half", s.getHalfDays());
            row.put("leave", s.getLeaveDays());
            row.put("absent", absent);
            row.put("attendance", s.getAttendanceRate());
            b.rows.add(row);

            totalExpected += s.getExpectedWorkingDays();
            totalAttended += attended;
            totalLate += s.getLateDays();
            if (s.getAttendanceRate() != null) {
                rateSum += s.getAttendanceRate();
                rateCount++;
            }
        }
        b.summary("Employees", String.valueOf(scores.size()));
        b.summary("Working days (total)", String.valueOf(totalExpected));
        b.summary("Days attended", String.valueOf(totalAttended));
        b.summary("Late arrivals", String.valueOf(totalLate));
        b.summary("Average attendance", rateCount > 0
                ? PerformanceMetricsService.round1(rateSum / rateCount) + "%" : "n/a");
    }

    private void buildLeave(ReportBuilder b, Long departmentId, DateRange range) {
        List<LeaveRequest> leaves = leaveRequestRepository.search(null, departmentId, null).stream()
                .filter(l -> !l.getStartDate().isBefore(range.from()) && !l.getStartDate().isAfter(range.to()))
                .sorted(Comparator.comparing(LeaveRequest::getStartDate).reversed())
                .toList();

        b.column("employeeId", "Employee ID", "text");
        b.column("name", "Name", "text");
        b.column("department", "Department", "text");
        b.column("type", "Leave Type", "text");
        b.column("start", "From", "text");
        b.column("end", "To", "text");
        b.column("days", "Days", "number");
        b.column("status", "Status", "text");
        b.column("applied", "Applied On", "text");

        int approvedDays = 0, pending = 0, rejected = 0, approved = 0;
        for (LeaveRequest leave : leaves) {
            Employee employee = leave.getEmployee();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("employeeId", employee.getEmployeeId());
            row.put("name", mapper.fullName(employee));
            row.put("department", employee.getDepartment().getDepartmentName());
            row.put("type", label(leave.getLeaveType().name()));
            row.put("start", leave.getStartDate().toString());
            row.put("end", leave.getEndDate().toString());
            row.put("days", leave.getNumberOfDays());
            row.put("status", leave.getStatus().name());
            row.put("applied", leave.getAppliedAt().toLocalDate().toString());
            b.rows.add(row);

            switch (leave.getStatus()) {
                case APPROVED -> { approved++; approvedDays += leave.getNumberOfDays(); }
                case PENDING -> pending++;
                case REJECTED -> rejected++;
                default -> { }
            }
        }
        b.summary("Requests", String.valueOf(leaves.size()));
        b.summary("Approved", String.valueOf(approved));
        b.summary("Approved days", String.valueOf(approvedDays));
        b.summary("Pending", String.valueOf(pending));
        b.summary("Rejected", String.valueOf(rejected));
    }

    private void buildDepartmentSummary(ReportBuilder b, User actor, Long departmentId, DateRange range) {
        List<Employee> employees = scopeService.activeEmployeesInScope(actor, departmentId);
        List<PerformanceScoreResponse> scores = metricsService.computeBulk(employees, range.from(), range.to());
        List<Reward> rewards = (departmentId == null)
                ? rewardRepository.findByAwardedDateBetween(range.from(), range.to())
                : rewardRepository.findByDepartmentIdAndAwardedDateBetween(departmentId, range.from(), range.to());

        // Group scores and rewards by department, preserving department-name order.
        Map<Long, List<PerformanceScoreResponse>> scoresByDept = new TreeMap<>();
        Map<Long, String> deptNames = new HashMap<>();
        for (PerformanceScoreResponse s : scores) {
            scoresByDept.computeIfAbsent(s.getDepartment().getId(), k -> new ArrayList<>()).add(s);
            deptNames.put(s.getDepartment().getId(), s.getDepartment().getDepartmentName());
        }
        Map<Long, Long> pointsByDept = new HashMap<>();
        Map<Long, Integer> rewardCountByDept = new HashMap<>();
        for (Reward r : rewards) {
            Long deptId = r.getEmployee().getDepartment().getId();
            pointsByDept.merge(deptId, (long) r.getPoints(), Long::sum);
            rewardCountByDept.merge(deptId, 1, Integer::sum);
            deptNames.putIfAbsent(deptId, r.getEmployee().getDepartment().getDepartmentName());
            scoresByDept.computeIfAbsent(deptId, k -> new ArrayList<>());
        }

        b.column("department", "Department", "text");
        b.column("employees", "Employees", "number");
        b.column("tasks", "Tasks", "number");
        b.column("delivered", "Delivered", "number");
        b.column("completion", "Completion (%)", "number");
        b.column("overdue", "Overdue", "number");
        b.column("attendance", "Attendance (%)", "number");
        b.column("score", "Avg Score", "number");
        b.column("rewards", "Rewards", "number");
        b.column("points", "Reward Points", "number");

        List<Long> orderedIds = new ArrayList<>(scoresByDept.keySet());
        orderedIds.sort(Comparator.comparing(id -> deptNames.get(id)));

        for (Long deptId : orderedIds) {
            List<PerformanceScoreResponse> group = scoresByDept.get(deptId);
            int tasks = group.stream().mapToInt(PerformanceScoreResponse::getTasksTotal).sum();
            int delivered = group.stream().mapToInt(PerformanceScoreResponse::getTasksDelivered).sum();
            int overdue = group.stream().mapToInt(PerformanceScoreResponse::getTasksOverdue).sum();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("department", deptNames.get(deptId));
            row.put("employees", group.size());
            row.put("tasks", tasks);
            row.put("delivered", delivered);
            row.put("completion", tasks > 0 ? PerformanceMetricsService.round1(delivered * 100.0 / tasks) : null);
            row.put("overdue", overdue);
            row.put("attendance", average(group.stream().map(PerformanceScoreResponse::getAttendanceRate).toList()));
            row.put("score", average(group.stream().map(PerformanceScoreResponse::getOverallScore).toList()));
            row.put("rewards", rewardCountByDept.getOrDefault(deptId, 0));
            row.put("points", pointsByDept.getOrDefault(deptId, 0L));
            b.rows.add(row);
        }

        b.summary("Departments", String.valueOf(orderedIds.size()));
        b.summary("Employees", String.valueOf(scores.size()));
        Double avg = average(scores.stream().map(PerformanceScoreResponse::getOverallScore).toList());
        b.summary("Average score", avg != null ? String.valueOf(avg) : "n/a");
        b.summary("Rewards granted", String.valueOf(rewards.size()));
    }

    private void buildRewards(ReportBuilder b, Long departmentId, DateRange range) {
        List<Reward> rewards = (departmentId == null)
                ? rewardRepository.findByAwardedDateBetween(range.from(), range.to())
                : rewardRepository.findByDepartmentIdAndAwardedDateBetween(departmentId, range.from(), range.to());

        b.column("date", "Date", "text");
        b.column("employeeId", "Employee ID", "text");
        b.column("name", "Employee", "text");
        b.column("department", "Department", "text");
        b.column("type", "Type", "text");
        b.column("title", "Title", "text");
        b.column("points", "Points", "number");
        b.column("awardedBy", "Awarded By", "text");

        long totalPoints = 0;
        Set<Long> distinctEmployees = new HashSet<>();
        for (Reward reward : rewards) {
            Employee employee = reward.getEmployee();
            User awardedBy = reward.getAwardedBy();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", reward.getAwardedDate().toString());
            row.put("employeeId", employee.getEmployeeId());
            row.put("name", mapper.fullName(employee));
            row.put("department", employee.getDepartment().getDepartmentName());
            row.put("type", reward.getType().getLabel());
            row.put("title", reward.getTitle());
            row.put("points", reward.getPoints());
            String awarder = mapper.user(awardedBy).getFullName();
            row.put("awardedBy", awarder != null ? awarder : awardedBy.getEmail());
            b.rows.add(row);

            totalPoints += reward.getPoints();
            distinctEmployees.add(employee.getId());
        }
        b.summary("Rewards granted", String.valueOf(rewards.size()));
        b.summary("Employees recognised", String.valueOf(distinctEmployees.size()));
        b.summary("Total points", String.valueOf(totalPoints));
    }

    // =========================================================================================
    // HELPERS
    // =========================================================================================

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

    /** "VERY_GOOD" -> "Very Good" */
    static String label(String enumName) {
        if (enumName == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String part : enumName.toLowerCase().split("_")) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private static final class ReportBuilder {
        final List<ReportColumn> columns = new ArrayList<>();
        final List<Map<String, Object>> rows = new ArrayList<>();
        final List<ReportSummaryItem> summary = new ArrayList<>();

        void column(String key, String label, String type) {
            columns.add(ReportColumn.builder().key(key).label(label).type(type).build());
        }

        void summary(String label, String value) {
            summary.add(ReportSummaryItem.builder().label(label).value(value).build());
        }
    }
}
