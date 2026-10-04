package com.govtrack360.config;

import com.govtrack360.entity.*;
import com.govtrack360.repository.*;
import com.govtrack360.service.AuditLogService;
import com.govtrack360.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds demo departments, employees, login accounts (Phase 1) and tasks, attendance,
 * leave requests, notifications and audit logs (Phase 2) on first startup only.
 * Safe to run repeatedly: it checks for existing data before inserting anything.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskWorkflowHistoryRepository workflowHistoryRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (departmentRepository.count() > 0 || userRepository.count() > 0) {
            return; // already seeded
        }

        // =====================================================================================
        // PHASE 1 SEED DATA (departments, users, employees) — unchanged from Phase 1
        // =====================================================================================

        Map<String, Department> departments = new LinkedHashMap<>();
        departments.put("ADMIN", saveDept("ADM", "Administration", "General administration and office management."));
        departments.put("IT", saveDept("IT", "Information Technology", "Manages IT infrastructure, systems and e-Governance platforms."));
        departments.put("FIN", saveDept("FIN", "Finance", "Handles budgeting, accounts and financial reporting."));
        departments.put("HR", saveDept("HR", "Human Resources", "Manages recruitment, employee records and welfare."));
        departments.put("PW", saveDept("PW", "Public Works", "Oversees infrastructure and public works projects."));

        User adminUser = saveUser("admin@govtrack360.com", "Admin@123", Role.ADMIN);
        User officerUser = saveUser("officer@govtrack360.com", "Officer@123", Role.OFFICER);
        User employeeUser = saveUser("employee@govtrack360.com", "Employee@123", Role.EMPLOYEE);

        Employee admin = saveEmployee("EMP-1001", "Ananya", "Sharma", "admin@govtrack360.com", "+91-9800000001",
                "System Administrator", departments.get("ADMIN"), LocalDate.of(2021, 1, 10), adminUser);

        Employee officer = saveEmployee("EMP-1002", "Rahul", "Verma", "officer@govtrack360.com", "+91-9800000002",
                "Administrative Officer", departments.get("IT"), LocalDate.of(2022, 3, 15), officerUser);

        Employee employee = saveEmployee("EMP-1003", "Priya", "Nair", "employee@govtrack360.com", "+91-9800000003",
                "Office Assistant", departments.get("HR"), LocalDate.of(2023, 6, 1), employeeUser);

        Employee karan = saveEmployee("EMP-1004", "Karan", "Mehta", "karan.mehta@govtrack360.com", "+91-9800000004",
                "Finance Officer", departments.get("FIN"), LocalDate.of(2020, 11, 20), null);

        Employee sneha = saveEmployee("EMP-1005", "Sneha", "Iyer", "sneha.iyer@govtrack360.com", "+91-9800000005",
                "Junior Engineer", departments.get("PW"), LocalDate.of(2024, 2, 5), null);

        Employee vikram = saveEmployee("EMP-1006", "Vikram", "Singh", "vikram.singh@govtrack360.com", "+91-9800000006",
                "HR Executive", departments.get("HR"), LocalDate.of(2022, 8, 18), null);

        Employee divya = saveEmployee("EMP-1007", "Divya", "Reddy", "divya.reddy@govtrack360.com", "+91-9800000007",
                "IT Support Engineer", departments.get("IT"), LocalDate.of(2023, 9, 12), null);

        Employee inactiveDemo = saveEmployee("EMP-1008", "Arjun", "Rao", "arjun.rao@govtrack360.com", "+91-9800000008",
                "Field Inspector", departments.get("PW"), LocalDate.of(2019, 4, 22), null);
        inactiveDemo.setStatus(Status.INACTIVE);
        employeeRepository.save(inactiveDemo);

        auditLogService.log(null, "SYSTEM_SEED", "SYSTEM", "Initial Phase 1 demo data seeded.");

        // =====================================================================================
        // PHASE 2 SEED DATA (tasks, attendance, leave, notifications, audit logs)
        // =====================================================================================

        LocalDate today = LocalDate.now();

        // ---- Tasks spanning every status, across departments and assignees ----

        Task t1 = saveTask("TSK-9001", "Prepare Q4 budget draft",
                "Draft the departmental budget proposal for the next quarter.",
                departments.get("FIN"), null, adminUser, TaskPriority.MEDIUM, TaskStatus.PENDING,
                today.minusDays(2), today.plusDays(10), 0, null, null, null, null);
        recordHistory(t1, "TASK_CREATED", null, TaskStatus.PENDING, adminUser, null);

        Task t2 = saveTask("TSK-9002", "Update employee handbook section 4",
                "Revise the leave policy section to reflect the new leave types.",
                departments.get("HR"), employee, adminUser, TaskPriority.LOW, TaskStatus.ASSIGNED,
                today.minusDays(5), today.plusDays(7), 0, null, null, null, null);
        recordHistory(t2, "TASK_CREATED", null, TaskStatus.ASSIGNED, adminUser, null);
        recordHistory(t2, "TASK_ASSIGNED", TaskStatus.PENDING, TaskStatus.ASSIGNED, adminUser, null);
        notifyIfLinked(employee, "Task update: TSK-9002", "You have been assigned a new task.", NotificationType.TASK);

        Task t3 = saveTask("TSK-9003", "Digitize old HR personnel files",
                "Scan and upload archived personnel records to the document system.",
                departments.get("HR"), employee, adminUser, TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS,
                today.minusDays(8), today.plusDays(5), 60, null, null, null, null);
        recordHistory(t3, "TASK_CREATED", null, TaskStatus.ASSIGNED, adminUser, null);
        recordHistory(t3, "TASK_ASSIGNED", TaskStatus.PENDING, TaskStatus.ASSIGNED, adminUser, null);
        recordHistory(t3, "TASK_STARTED", TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, employeeUser, null);
        recordHistory(t3, "PROGRESS_UPDATED", TaskStatus.IN_PROGRESS, TaskStatus.IN_PROGRESS, employeeUser, "60% of files digitized so far.");
        saveComment(t3, employeeUser, "About 3 more boxes of archived files to go.");

        Task t4 = saveTask("TSK-9004", "Draft new employee onboarding checklist",
                "Create a step-by-step onboarding checklist for new government staff.",
                departments.get("HR"), employee, adminUser, TaskPriority.HIGH, TaskStatus.SUBMITTED,
                today.minusDays(10), today.minusDays(1), 100, LocalDateTime.now().minusHours(6), null, null, null);
        recordHistory(t4, "TASK_CREATED", null, TaskStatus.ASSIGNED, adminUser, null);
        recordHistory(t4, "TASK_ASSIGNED", TaskStatus.PENDING, TaskStatus.ASSIGNED, adminUser, null);
        recordHistory(t4, "TASK_STARTED", TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, employeeUser, null);
        recordHistory(t4, "TASK_SUBMITTED", TaskStatus.IN_PROGRESS, TaskStatus.SUBMITTED, employeeUser, "Ready for review.");
        notificationService.notify(adminUser, "Task submitted for review",
                "Task TSK-9004 (Draft new employee onboarding checklist) has been submitted for review.", NotificationType.TASK);

        Task t5 = saveTask("TSK-9005", "Organize Q3 recruitment drive",
                "Coordinate interviews and paperwork for the Q3 recruitment cycle.",
                departments.get("HR"), vikram, adminUser, TaskPriority.MEDIUM, TaskStatus.APPROVED,
                today.minusDays(20), today.minusDays(5), 100, LocalDateTime.now().minusDays(4),
                LocalDateTime.now().minusDays(3), adminUser, "Well done, approved.");
        recordHistory(t5, "TASK_CREATED", null, TaskStatus.ASSIGNED, adminUser, null);
        recordHistory(t5, "TASK_ASSIGNED", TaskStatus.PENDING, TaskStatus.ASSIGNED, adminUser, null);
        recordHistory(t5, "TASK_SUBMITTED", TaskStatus.IN_PROGRESS, TaskStatus.SUBMITTED, adminUser, null);
        recordHistory(t5, "UNDER_REVIEW", TaskStatus.SUBMITTED, TaskStatus.UNDER_REVIEW, adminUser, null);
        recordHistory(t5, "TASK_APPROVED", TaskStatus.UNDER_REVIEW, TaskStatus.APPROVED, adminUser, "Well done, approved.");

        Task t6 = saveTask("TSK-9006", "Reconcile Q3 departmental accounts",
                "Cross-check Q3 expenditure entries against departmental receipts.",
                departments.get("FIN"), karan, adminUser, TaskPriority.HIGH, TaskStatus.REJECTED,
                today.minusDays(15), today.minusDays(3), 90, LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1), adminUser, "Please recheck the reconciliation figures for August.");
        recordHistory(t6, "TASK_CREATED", null, TaskStatus.ASSIGNED, adminUser, null);
        recordHistory(t6, "TASK_ASSIGNED", TaskStatus.PENDING, TaskStatus.ASSIGNED, adminUser, null);
        recordHistory(t6, "TASK_SUBMITTED", TaskStatus.IN_PROGRESS, TaskStatus.SUBMITTED, adminUser, null);
        recordHistory(t6, "UNDER_REVIEW", TaskStatus.SUBMITTED, TaskStatus.UNDER_REVIEW, adminUser, null);
        recordHistory(t6, "TASK_REJECTED", TaskStatus.UNDER_REVIEW, TaskStatus.REJECTED, adminUser,
                "Please recheck the reconciliation figures for August.");

        Task t7 = saveTask("TSK-9007", "Deploy new ticketing system",
                "Roll out the internal IT support ticketing system to all departments.",
                departments.get("IT"), divya, officerUser, TaskPriority.MEDIUM, TaskStatus.SUBMITTED,
                today.minusDays(9), today.minusDays(1), 100, LocalDateTime.now().minusHours(10), null, null, null);
        recordHistory(t7, "TASK_CREATED", null, TaskStatus.ASSIGNED, officerUser, null);
        recordHistory(t7, "TASK_ASSIGNED", TaskStatus.PENDING, TaskStatus.ASSIGNED, officerUser, null);
        recordHistory(t7, "TASK_STARTED", TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, officerUser, null);
        recordHistory(t7, "TASK_SUBMITTED", TaskStatus.IN_PROGRESS, TaskStatus.SUBMITTED, officerUser, "Deployed to pilot group, ready for review.");
        notificationService.notify(officerUser, "Task submitted for review",
                "Task TSK-9007 (Deploy new ticketing system) has been submitted for review.", NotificationType.TASK);

        Task t8 = saveTask("TSK-9008", "Patch office workstations",
                "Apply the latest security patches to all IT department workstations.",
                departments.get("IT"), divya, officerUser, TaskPriority.LOW, TaskStatus.IN_PROGRESS,
                today.minusDays(3), today.plusDays(6), 30, null, null, null, null);
        recordHistory(t8, "TASK_CREATED", null, TaskStatus.ASSIGNED, officerUser, null);
        recordHistory(t8, "TASK_ASSIGNED", TaskStatus.PENDING, TaskStatus.ASSIGNED, officerUser, null);
        recordHistory(t8, "TASK_STARTED", TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, officerUser, "12 of 40 machines patched so far.");

        for (Task t : new Task[]{t1, t2, t3, t4, t5, t6, t7, t8}) {
            auditLogService.log(t.getAssignedBy(), "TASK_CREATED", "TASK", "Seeded task " + t.getTaskCode() + " (" + t.getTitle() + ")");
        }

        // ---- Attendance history (recent days, not today — check-in/out is demonstrated live) ----

        saveAttendance(employee, today.minusDays(1), LocalTime.of(9, 10), LocalTime.of(17, 45), AttendanceStatus.PRESENT);
        saveAttendance(employee, today.minusDays(2), LocalTime.of(10, 5), LocalTime.of(17, 50), AttendanceStatus.LATE);
        saveAttendance(divya, today.minusDays(1), LocalTime.of(9, 0), LocalTime.of(17, 30), AttendanceStatus.PRESENT);
        saveAttendance(divya, today.minusDays(2), LocalTime.of(9, 15), LocalTime.of(12, 0), AttendanceStatus.HALF_DAY);
        saveAttendance(officer, today.minusDays(1), LocalTime.of(8, 55), LocalTime.of(18, 0), AttendanceStatus.PRESENT);
        saveAttendance(admin, today.minusDays(1), LocalTime.of(9, 0), LocalTime.of(17, 40), AttendanceStatus.PRESENT);

        auditLogService.log(null, "SYSTEM_SEED", "ATTENDANCE", "Seeded recent attendance history for demo employees.");

        // ---- Leave requests ----

        LeaveRequest approvedLeave = LeaveRequest.builder()
                .employee(vikram)
                .leaveType(LeaveType.EARNED_LEAVE)
                .startDate(today.minusDays(25))
                .endDate(today.minusDays(24))
                .numberOfDays(2)
                .reason("Family function out of town.")
                .status(LeaveStatus.APPROVED)
                .reviewedBy(adminUser)
                .reviewerComment("Approved. Enjoy your time off.")
                .build();
        approvedLeave = leaveRequestRepository.save(approvedLeave);
        approvedLeave.setReviewedAt(LocalDateTime.now().minusDays(23));
        leaveRequestRepository.save(approvedLeave);
        auditLogService.log(adminUser, "LEAVE_APPROVED", "LEAVE", "Seeded approved leave request for " + vikram.getEmployeeId());

        LeaveRequest pendingLeaveDivya = LeaveRequest.builder()
                .employee(divya)
                .leaveType(LeaveType.CASUAL_LEAVE)
                .startDate(today.plusDays(7))
                .endDate(today.plusDays(8))
                .numberOfDays(2)
                .reason("Personal family function.")
                .status(LeaveStatus.PENDING)
                .build();
        leaveRequestRepository.save(pendingLeaveDivya);
        auditLogService.log(null, "LEAVE_APPLIED", "LEAVE", "Seeded pending leave request for " + divya.getEmployeeId());

        LeaveRequest pendingLeaveEmployee = LeaveRequest.builder()
                .employee(employee)
                .leaveType(LeaveType.SICK_LEAVE)
                .startDate(today.plusDays(5))
                .endDate(today.plusDays(5))
                .numberOfDays(1)
                .reason("Doctor's appointment.")
                .status(LeaveStatus.PENDING)
                .build();
        leaveRequestRepository.save(pendingLeaveEmployee);
        auditLogService.log(null, "LEAVE_APPLIED", "LEAVE", "Seeded pending leave request for " + employee.getEmployeeId());

        // ---- Welcome notifications ----

        notificationService.notify(adminUser, "Welcome to GovTrack 360 Phase 2",
                "Explore Task, Attendance, Leave, Notification and Audit Log management.", NotificationType.SYSTEM);
        notificationService.notify(officerUser, "Welcome to GovTrack 360 Phase 2",
                "Explore Task, Attendance and Leave management for your department.", NotificationType.SYSTEM);
        notificationService.notify(employeeUser, "Welcome to GovTrack 360 Phase 2",
                "Track your tasks, attendance and leave requests here.", NotificationType.SYSTEM);
    }

    // =========================================================================================
    // Phase 1 helpers (unchanged)
    // =========================================================================================

    private Department saveDept(String code, String name, String description) {
        return departmentRepository.save(Department.builder()
                .departmentCode(code)
                .departmentName(name)
                .description(description)
                .status(Status.ACTIVE)
                .build());
    }

    private User saveUser(String email, String rawPassword, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .enabled(true)
                .build());
    }

    private Employee saveEmployee(String employeeId, String firstName, String lastName, String email, String phone,
                                   String designation, Department department, LocalDate joiningDate, User user) {
        return employeeRepository.save(Employee.builder()
                .employeeId(employeeId)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .designation(designation)
                .department(department)
                .joiningDate(joiningDate)
                .status(Status.ACTIVE)
                .user(user)
                .build());
    }

    // =========================================================================================
    // Phase 2 helpers
    // =========================================================================================

    private Task saveTask(String taskCode, String title, String description, Department department,
                           Employee assignedTo, User assignedBy, TaskPriority priority, TaskStatus status,
                           LocalDate startDate, LocalDate dueDate, int progress,
                           LocalDateTime submittedAt, LocalDateTime reviewedAt, User reviewedBy, String reviewComment) {
        Task task = Task.builder()
                .taskCode(taskCode)
                .title(title)
                .description(description)
                .department(department)
                .assignedTo(assignedTo)
                .assignedBy(assignedBy)
                .priority(priority)
                .status(status)
                .startDate(startDate)
                .dueDate(dueDate)
                .progressPercentage(progress)
                .submittedAt(submittedAt)
                .reviewedAt(reviewedAt)
                .reviewedBy(reviewedBy)
                .reviewComment(reviewComment)
                .build();
        return taskRepository.save(task);
    }

    private void recordHistory(Task task, String action, TaskStatus previousStatus, TaskStatus newStatus,
                                User performedBy, String comment) {
        workflowHistoryRepository.save(TaskWorkflowHistory.builder()
                .task(task)
                .action(action)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .performedBy(performedBy)
                .comment(comment)
                .build());
    }

    private void saveComment(Task task, User author, String comment) {
        taskCommentRepository.save(TaskComment.builder()
                .task(task)
                .author(author)
                .comment(comment)
                .build());
    }

    private void saveAttendance(Employee employee, LocalDate date, LocalTime checkIn, LocalTime checkOut, AttendanceStatus status) {
        double hours = Math.round((java.time.Duration.between(checkIn, checkOut).toMinutes() / 60.0) * 100.0) / 100.0;
        attendanceRepository.save(Attendance.builder()
                .employee(employee)
                .date(date)
                .checkInTime(checkIn)
                .checkOutTime(checkOut)
                .totalWorkingHours(hours)
                .status(status)
                .build());
    }

    private void notifyIfLinked(Employee employee, String title, String message, NotificationType type) {
        if (employee.getUser() != null) {
            notificationService.notify(employee.getUser(), title, message, type);
        }
    }
}
