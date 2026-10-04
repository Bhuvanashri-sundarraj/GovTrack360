package com.govtrack360.service;

import com.govtrack360.dto.common.EmployeeSummary;
import com.govtrack360.dto.common.UserSummary;
import com.govtrack360.dto.department.DepartmentSummary;
import com.govtrack360.dto.task.*;
import com.govtrack360.entity.*;
import com.govtrack360.exception.InvalidRequestException;
import com.govtrack360.exception.ResourceNotFoundException;
import com.govtrack360.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskWorkflowHistoryRepository workflowHistoryRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    private static final Set<TaskStatus> ASSIGNABLE_FROM =
            Set.of(TaskStatus.PENDING, TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS);

    private static final Set<TaskStatus> PROGRESS_UPDATABLE_FROM =
            Set.of(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, TaskStatus.REJECTED);

    private static final Set<TaskStatus> CANCELLABLE_FROM =
            Set.of(TaskStatus.PENDING, TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS,
                    TaskStatus.SUBMITTED, TaskStatus.UNDER_REVIEW, TaskStatus.REJECTED);

    private static final Set<TaskStatus> EDITABLE_FROM =
            Set.of(TaskStatus.PENDING, TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, TaskStatus.REJECTED);

    // =========================================================================================
    // CREATE
    // =========================================================================================

    @Transactional
    public TaskResponse create(TaskCreateRequest request, User actor) {
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        enforceOfficerOwnDepartment(actor, department.getId());

        if (request.getStartDate().isAfter(request.getDueDate())) {
            throw new InvalidRequestException("Start date cannot be after the due date.");
        }

        Employee assignedTo = null;
        if (request.getAssignedToEmployeeId() != null) {
            assignedTo = employeeRepository.findById(request.getAssignedToEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getAssignedToEmployeeId()));
            if (!assignedTo.getDepartment().getId().equals(department.getId())) {
                throw new InvalidRequestException("The assigned employee must belong to the selected department.");
            }
        }

        TaskStatus initialStatus = (assignedTo != null) ? TaskStatus.ASSIGNED : TaskStatus.PENDING;

        Task task = Task.builder()
                .taskCode(generateTaskCode())
                .title(request.getTitle())
                .description(request.getDescription())
                .department(department)
                .assignedTo(assignedTo)
                .assignedBy(actor)
                .priority(request.getPriority())
                .status(initialStatus)
                .startDate(request.getStartDate())
                .dueDate(request.getDueDate())
                .progressPercentage(0)
                .build();

        task = taskRepository.save(task);

        recordHistory(task, "TASK_CREATED", null, task.getStatus(), actor, null);

        if (assignedTo != null) {
            recordHistory(task, "TASK_ASSIGNED", TaskStatus.PENDING, TaskStatus.ASSIGNED, actor, null);
            notifyAssignee(task, "You have been assigned a new task.");
        }

        auditLogService.log(actor, "TASK_CREATED", "TASK",
                "Created task " + task.getTaskCode() + " (" + task.getTitle() + ")");

        return toResponse(task);
    }

    // =========================================================================================
    // UPDATE (editable fields only; not assignee/status)
    // =========================================================================================

    @Transactional
    public TaskResponse update(Long id, TaskUpdateRequest request, User actor) {
        Task task = findEntity(id);
        enforceOfficerOwnDepartment(actor, task.getDepartment().getId());

        if (!EDITABLE_FROM.contains(task.getStatus())) {
            throw new InvalidRequestException("This task can no longer be edited in its current status.");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
        enforceOfficerOwnDepartment(actor, department.getId());

        if (request.getStartDate().isAfter(request.getDueDate())) {
            throw new InvalidRequestException("Start date cannot be after the due date.");
        }

        if (task.getAssignedTo() != null && !task.getAssignedTo().getDepartment().getId().equals(department.getId())) {
            throw new InvalidRequestException("Cannot move this task to a department the current assignee does not belong to. Reassign the task first.");
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDepartment(department);
        task.setPriority(request.getPriority());
        task.setStartDate(request.getStartDate());
        task.setDueDate(request.getDueDate());

        task = taskRepository.save(task);
        auditLogService.log(actor, "TASK_UPDATED", "TASK", "Updated task " + task.getTaskCode());

        return toResponse(task);
    }

    // =========================================================================================
    // ASSIGN / REASSIGN
    // =========================================================================================

    @Transactional
    public TaskResponse assign(Long id, TaskAssignRequest request, User actor) {
        Task task = findEntity(id);
        enforceOfficerOwnDepartment(actor, task.getDepartment().getId());

        if (!ASSIGNABLE_FROM.contains(task.getStatus())) {
            throw new InvalidRequestException("This task cannot be assigned in its current status.");
        }

        Employee newAssignee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));

        if (!newAssignee.getDepartment().getId().equals(task.getDepartment().getId())) {
            throw new InvalidRequestException("The assigned employee must belong to the task's department.");
        }

        boolean isReassignment = task.getAssignedTo() != null && !task.getAssignedTo().getId().equals(newAssignee.getId());
        TaskStatus previousStatus = task.getStatus();

        task.setAssignedTo(newAssignee);
        task.setStatus(TaskStatus.ASSIGNED);
        if (isReassignment) {
            task.setProgressPercentage(0);
        }

        task = taskRepository.save(task);

        String action = isReassignment ? "TASK_REASSIGNED" : "TASK_ASSIGNED";
        recordHistory(task, action, previousStatus, TaskStatus.ASSIGNED, actor, request.getComment());
        notifyAssignee(task, "You have been assigned a new task.");
        auditLogService.log(actor, action, "TASK", action + ": " + task.getTaskCode());

        return toResponse(task);
    }

    // =========================================================================================
    // PROGRESS UPDATE (employee, assignee only)
    // =========================================================================================

    @Transactional
    public TaskResponse updateProgress(Long id, TaskProgressRequest request, User actor) {
        Task task = findEntity(id);
        Employee actingEmployee = requireOwnEmployee(actor);

        if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(actingEmployee.getId())) {
            throw new AccessDeniedException("You can only update progress on tasks assigned to you.");
        }

        if (!PROGRESS_UPDATABLE_FROM.contains(task.getStatus())) {
            throw new InvalidRequestException("Progress can only be updated while a task is assigned, in progress, or being reworked after rejection.");
        }

        TaskStatus previousStatus = task.getStatus();
        String action;
        if (previousStatus == TaskStatus.ASSIGNED) {
            action = "TASK_STARTED";
        } else if (previousStatus == TaskStatus.REJECTED) {
            action = "TASK_REOPENED";
        } else {
            action = "PROGRESS_UPDATED";
        }

        task.setProgressPercentage(request.getProgressPercentage());
        task.setStatus(TaskStatus.IN_PROGRESS);

        task = taskRepository.save(task);
        recordHistory(task, action, previousStatus, TaskStatus.IN_PROGRESS, actor, request.getComment());

        return toResponse(task);
    }

    // =========================================================================================
    // SUBMIT (employee, assignee only)
    // =========================================================================================

    @Transactional
    public TaskResponse submit(Long id, String comment, User actor) {
        Task task = findEntity(id);
        Employee actingEmployee = requireOwnEmployee(actor);

        if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(actingEmployee.getId())) {
            throw new AccessDeniedException("You can only submit tasks assigned to you.");
        }

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new InvalidRequestException("Only tasks that are in progress can be submitted.");
        }

        if (task.getProgressPercentage() < 100) {
            throw new InvalidRequestException("Set progress to 100% before submitting the task.");
        }

        TaskStatus previousStatus = task.getStatus();
        task.setStatus(TaskStatus.SUBMITTED);
        task.setSubmittedAt(java.time.LocalDateTime.now());

        task = taskRepository.save(task);
        recordHistory(task, "TASK_SUBMITTED", previousStatus, TaskStatus.SUBMITTED, actor, comment);

        if (task.getAssignedBy() != null) {
            notificationService.notify(task.getAssignedBy(), "Task submitted for review",
                    "Task " + task.getTaskCode() + " (" + task.getTitle() + ") has been submitted for review.",
                    NotificationType.TASK);
        }

        auditLogService.log(actor, "TASK_SUBMITTED", "TASK", "Submitted task " + task.getTaskCode());

        return toResponse(task);
    }

    // =========================================================================================
    // REVIEW (ADMIN/OFFICER only; approve or reject)
    // =========================================================================================

    @Transactional
    public TaskResponse review(Long id, TaskReviewRequest request, User actor) {
        Task task = findEntity(id);
        enforceOfficerOwnDepartment(actor, task.getDepartment().getId());

        if (task.getStatus() != TaskStatus.SUBMITTED && task.getStatus() != TaskStatus.UNDER_REVIEW) {
            throw new InvalidRequestException("Only submitted tasks can be reviewed.");
        }

        if (task.getStatus() == TaskStatus.SUBMITTED) {
            TaskStatus prev = task.getStatus();
            task.setStatus(TaskStatus.UNDER_REVIEW);
            task = taskRepository.save(task);
            recordHistory(task, "UNDER_REVIEW", prev, TaskStatus.UNDER_REVIEW, actor, null);
        }

        String decision = request.getDecision() == null ? "" : request.getDecision().trim().toUpperCase();
        if (!decision.equals("APPROVE") && !decision.equals("REJECT")) {
            throw new InvalidRequestException("Decision must be either APPROVE or REJECT.");
        }

        if (decision.equals("REJECT") && (request.getComment() == null || request.getComment().isBlank())) {
            throw new InvalidRequestException("A comment is required when rejecting a task.");
        }

        TaskStatus previousStatus = task.getStatus();
        task.setReviewedAt(java.time.LocalDateTime.now());
        task.setReviewedBy(actor);
        task.setReviewComment(request.getComment());

        if (decision.equals("APPROVE")) {
            task.setStatus(TaskStatus.APPROVED);
            task = taskRepository.save(task);
            recordHistory(task, "TASK_APPROVED", previousStatus, TaskStatus.APPROVED, actor, request.getComment());
            notifyAssignee(task, "Your task has been approved.");
            auditLogService.log(actor, "TASK_APPROVED", "TASK", "Approved task " + task.getTaskCode());
        } else {
            task.setStatus(TaskStatus.REJECTED);
            task = taskRepository.save(task);
            recordHistory(task, "TASK_REJECTED", previousStatus, TaskStatus.REJECTED, actor, request.getComment());
            notifyAssignee(task, "Your task requires changes: " + request.getComment());
            auditLogService.log(actor, "TASK_REJECTED", "TASK", "Rejected task " + task.getTaskCode());
        }

        return toResponse(task);
    }

    // =========================================================================================
    // CANCEL (status change endpoint; only CANCELLED is supported here)
    // =========================================================================================

    @Transactional
    public TaskResponse changeStatus(Long id, TaskStatusChangeRequest request, User actor) {
        Task task = findEntity(id);
        enforceOfficerOwnDepartment(actor, task.getDepartment().getId());

        String target = request.getStatus() == null ? "" : request.getStatus().trim().toUpperCase();
        if (!target.equals("CANCELLED")) {
            throw new InvalidRequestException("This endpoint only supports cancelling a task. Use the dedicated assign/submit/review endpoints for other transitions.");
        }

        if (!CANCELLABLE_FROM.contains(task.getStatus())) {
            throw new InvalidRequestException("This task can no longer be cancelled from its current status.");
        }

        TaskStatus previousStatus = task.getStatus();
        task.setStatus(TaskStatus.CANCELLED);

        task = taskRepository.save(task);
        recordHistory(task, "TASK_CANCELLED", previousStatus, TaskStatus.CANCELLED, actor, request.getComment());
        auditLogService.log(actor, "TASK_CANCELLED", "TASK", "Cancelled task " + task.getTaskCode());

        return toResponse(task);
    }

    // =========================================================================================
    // DELETE (ADMIN only; PENDING or CANCELLED tasks only)
    // =========================================================================================

    @Transactional
    public void delete(Long id, User actor) {
        Task task = findEntity(id);

        if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.CANCELLED) {
            throw new InvalidRequestException("Only pending or cancelled tasks can be deleted. Cancel the task first if needed.");
        }

        workflowHistoryRepository.deleteAll(workflowHistoryRepository.findByTaskOrderByTimestampAsc(task));
        taskCommentRepository.deleteAll(taskCommentRepository.findByTaskOrderByCreatedAtAsc(task));

        String taskCode = task.getTaskCode();
        taskRepository.delete(task);

        auditLogService.log(actor, "TASK_DELETED", "TASK", "Deleted task " + taskCode);
    }

    // =========================================================================================
    // READ
    // =========================================================================================

    @Transactional(readOnly = true)
    public TaskResponse getById(Long id, User actor) {
        Task task = findEntity(id);
        assertCanView(task, actor);
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> search(String search, TaskStatus status, TaskPriority priority,
                                      Long departmentId, Long assignedToId, User actor) {
        Long restrictDepartmentId = null;
        Long restrictAssignedToId = null;

        if (actor.getRole() == Role.OFFICER) {
            Employee officerEmployee = employeeRepository.findByUser(actor).orElse(null);
            if (officerEmployee == null) {
                return List.of();
            }
            restrictDepartmentId = officerEmployee.getDepartment().getId();
        } else if (actor.getRole() == Role.EMPLOYEE) {
            Employee employee = employeeRepository.findByUser(actor).orElse(null);
            if (employee == null) {
                return List.of();
            }
            restrictAssignedToId = employee.getId();
        }

        return taskRepository.search(
                (search == null || search.isBlank()) ? "" : search.trim(), // "" not null: null breaks LOWER() on PostgreSQL
                status, priority, departmentId, assignedToId,
                restrictDepartmentId, restrictAssignedToId
        ).stream().map(this::toResponse).toList();
    }

    // =========================================================================================
    // COMMENTS
    // =========================================================================================

    @Transactional(readOnly = true)
    public List<TaskCommentResponse> getComments(Long taskId, User actor) {
        Task task = findEntity(taskId);
        assertCanView(task, actor);
        return taskCommentRepository.findByTaskOrderByCreatedAtAsc(task).stream()
                .map(c -> TaskCommentResponse.builder()
                        .id(c.getId())
                        .author(buildUserSummary(c.getAuthor()))
                        .comment(c.getComment())
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public TaskCommentResponse addComment(Long taskId, TaskCommentRequest request, User actor) {
        Task task = findEntity(taskId);
        assertCanView(task, actor);

        TaskComment comment = TaskComment.builder()
                .task(task)
                .author(actor)
                .comment(request.getComment())
                .build();
        comment = taskCommentRepository.save(comment);

        return TaskCommentResponse.builder()
                .id(comment.getId())
                .author(buildUserSummary(actor))
                .comment(comment.getComment())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    // =========================================================================================
    // WORKFLOW HISTORY
    // =========================================================================================

    @Transactional(readOnly = true)
    public List<TaskWorkflowHistoryResponse> getHistory(Long taskId, User actor) {
        Task task = findEntity(taskId);
        assertCanView(task, actor);
        return workflowHistoryRepository.findByTaskOrderByTimestampAsc(task).stream()
                .map(h -> TaskWorkflowHistoryResponse.builder()
                        .id(h.getId())
                        .action(h.getAction())
                        .previousStatus(h.getPreviousStatus() != null ? h.getPreviousStatus().name() : null)
                        .newStatus(h.getNewStatus().name())
                        .performedBy(buildUserSummary(h.getPerformedBy()))
                        .comment(h.getComment())
                        .timestamp(h.getTimestamp())
                        .build())
                .toList();
    }

    // =========================================================================================
    // Internal helpers
    // =========================================================================================

    private Task findEntity(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private Employee requireOwnEmployee(User actor) {
        return employeeRepository.findByUser(actor)
                .orElseThrow(() -> new InvalidRequestException("No employee record is linked to your account. Contact an administrator."));
    }

    private void enforceOfficerOwnDepartment(User actor, Long departmentId) {
        if (actor.getRole() != Role.OFFICER) {
            return; // ADMIN is unrestricted; EMPLOYEE never reaches these management methods (blocked by @PreAuthorize)
        }
        Employee officerEmployee = employeeRepository.findByUser(actor).orElse(null);
        if (officerEmployee == null || !officerEmployee.getDepartment().getId().equals(departmentId)) {
            throw new AccessDeniedException("Officers may only manage tasks within their own department.");
        }
    }

    private void assertCanView(Task task, User actor) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        if (actor.getRole() == Role.OFFICER) {
            Employee officerEmployee = employeeRepository.findByUser(actor).orElse(null);
            if (officerEmployee != null && officerEmployee.getDepartment().getId().equals(task.getDepartment().getId())) {
                return;
            }
            throw new AccessDeniedException("You do not have access to this task.");
        }
        // EMPLOYEE
        Employee employee = employeeRepository.findByUser(actor).orElse(null);
        if (employee != null && task.getAssignedTo() != null && task.getAssignedTo().getId().equals(employee.getId())) {
            return;
        }
        throw new AccessDeniedException("You do not have access to this task.");
    }

    private void notifyAssignee(Task task, String message) {
        if (task.getAssignedTo() != null && task.getAssignedTo().getUser() != null) {
            notificationService.notify(task.getAssignedTo().getUser(), "Task update: " + task.getTaskCode(),
                    message, NotificationType.TASK);
        }
    }

    private void recordHistory(Task task, String action, TaskStatus previousStatus, TaskStatus newStatus,
                                User performedBy, String comment) {
        TaskWorkflowHistory history = TaskWorkflowHistory.builder()
                .task(task)
                .action(action)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .performedBy(performedBy)
                .comment(comment)
                .build();
        workflowHistoryRepository.save(history);
    }

    private String generateTaskCode() {
        String code;
        long sequence = taskRepository.count() + 1001;
        do {
            code = "TSK-" + sequence;
            sequence++;
        } while (taskRepository.existsByTaskCode(code));
        return code;
    }

    private UserSummary buildUserSummary(User user) {
        if (user == null) {
            return null;
        }
        Optional<Employee> employee = employeeRepository.findByUser(user);
        return UserSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(employee.map(e -> e.getFirstName() + " " + e.getLastName()).orElse(null))
                .build();
    }

    private EmployeeSummary buildEmployeeSummary(Employee employee) {
        if (employee == null) {
            return null;
        }
        return EmployeeSummary.builder()
                .id(employee.getId())
                .employeeId(employee.getEmployeeId())
                .fullName(employee.getFirstName() + " " + employee.getLastName())
                .designation(employee.getDesignation())
                .build();
    }

    private TaskResponse toResponse(Task task) {
        boolean overdue = task.getDueDate() != null
                && task.getDueDate().isBefore(LocalDate.now())
                && task.getStatus() != TaskStatus.APPROVED
                && task.getStatus() != TaskStatus.CANCELLED;

        return TaskResponse.builder()
                .id(task.getId())
                .taskCode(task.getTaskCode())
                .title(task.getTitle())
                .description(task.getDescription())
                .department(DepartmentSummary.builder()
                        .id(task.getDepartment().getId())
                        .departmentCode(task.getDepartment().getDepartmentCode())
                        .departmentName(task.getDepartment().getDepartmentName())
                        .status(task.getDepartment().getStatus().name())
                        .build())
                .assignedTo(buildEmployeeSummary(task.getAssignedTo()))
                .assignedBy(buildUserSummary(task.getAssignedBy()))
                .priority(task.getPriority().name())
                .status(task.getStatus().name())
                .startDate(task.getStartDate())
                .dueDate(task.getDueDate())
                .progressPercentage(task.getProgressPercentage())
                .overdue(overdue)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .submittedAt(task.getSubmittedAt())
                .reviewedAt(task.getReviewedAt())
                .reviewedBy(buildUserSummary(task.getReviewedBy()))
                .reviewComment(task.getReviewComment())
                .build();
    }
}
