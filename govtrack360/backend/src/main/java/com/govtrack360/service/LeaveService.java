package com.govtrack360.service;

import com.govtrack360.dto.common.EmployeeSummary;
import com.govtrack360.dto.common.UserSummary;
import com.govtrack360.dto.leave.LeaveRequestCreateRequest;
import com.govtrack360.dto.leave.LeaveRequestResponse;
import com.govtrack360.dto.leave.LeaveReviewRequest;
import com.govtrack360.entity.*;
import com.govtrack360.exception.InvalidRequestException;
import com.govtrack360.exception.ResourceNotFoundException;
import com.govtrack360.repository.EmployeeRepository;
import com.govtrack360.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    // =========================================================================================
    // EMPLOYEE SELF-SERVICE
    // =========================================================================================

    @Transactional
    public LeaveRequestResponse apply(LeaveRequestCreateRequest request, User actor) {
        Employee employee = requireOwnEmployee(actor);

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new InvalidRequestException("Start date cannot be after the end date.");
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new InvalidRequestException("Leave cannot be applied for a date in the past.");
        }

        int numberOfDays = (int) (ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1);

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .numberOfDays(numberOfDays)
                .reason(request.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        leaveRequest = leaveRequestRepository.save(leaveRequest);
        auditLogService.log(actor, "LEAVE_APPLIED", "LEAVE",
                employee.getEmployeeId() + " applied for " + numberOfDays + " day(s) of " + request.getLeaveType());

        return toResponse(leaveRequest);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getMy(User actor) {
        Employee employee = requireOwnEmployee(actor);
        return leaveRequestRepository.findByEmployeeOrderByAppliedAtDesc(employee)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public LeaveRequestResponse cancel(Long id, User actor) {
        LeaveRequest leaveRequest = findEntity(id);
        Employee employee = requireOwnEmployee(actor);

        if (!leaveRequest.getEmployee().getId().equals(employee.getId())) {
            throw new AccessDeniedException("You can only cancel your own leave requests.");
        }
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidRequestException("Only pending leave requests can be cancelled.");
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        leaveRequest = leaveRequestRepository.save(leaveRequest);
        auditLogService.log(actor, "LEAVE_CANCELLED", "LEAVE",
                employee.getEmployeeId() + " cancelled their leave request #" + leaveRequest.getId());

        return toResponse(leaveRequest);
    }

    // =========================================================================================
    // ADMIN / OFFICER
    // =========================================================================================

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> search(Long employeeId, Long departmentId, LeaveStatus status, User actor) {
        Long effectiveDepartmentId = departmentId;

        if (actor.getRole() == Role.OFFICER) {
            Employee officerEmployee = employeeRepository.findByUser(actor).orElse(null);
            if (officerEmployee == null) {
                return List.of();
            }
            Long officerDeptId = officerEmployee.getDepartment().getId();
            if (departmentId != null && !departmentId.equals(officerDeptId)) {
                throw new AccessDeniedException("Officers may only view leave requests within their own department.");
            }
            effectiveDepartmentId = officerDeptId;
        }

        return leaveRequestRepository.search(employeeId, effectiveDepartmentId, status)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public LeaveRequestResponse review(Long id, LeaveReviewRequest request, User actor) {
        LeaveRequest leaveRequest = findEntity(id);

        if (actor.getRole() == Role.OFFICER) {
            Employee officerEmployee = employeeRepository.findByUser(actor).orElse(null);
            if (officerEmployee == null
                    || !officerEmployee.getDepartment().getId().equals(leaveRequest.getEmployee().getDepartment().getId())) {
                throw new AccessDeniedException("Officers may only review leave requests within their own department.");
            }
        }

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidRequestException("Only pending leave requests can be reviewed.");
        }

        String decision = request.getDecision() == null ? "" : request.getDecision().trim().toUpperCase();
        if (!decision.equals("APPROVE") && !decision.equals("REJECT")) {
            throw new InvalidRequestException("Decision must be either APPROVE or REJECT.");
        }
        if (decision.equals("REJECT") && (request.getComment() == null || request.getComment().isBlank())) {
            throw new InvalidRequestException("A comment is required when rejecting a leave request.");
        }

        leaveRequest.setReviewedAt(LocalDateTime.now());
        leaveRequest.setReviewedBy(actor);
        leaveRequest.setReviewerComment(request.getComment());

        Employee employee = leaveRequest.getEmployee();

        if (decision.equals("APPROVE")) {
            leaveRequest.setStatus(LeaveStatus.APPROVED);
            leaveRequest = leaveRequestRepository.save(leaveRequest);
            if (employee.getUser() != null) {
                notificationService.notify(employee.getUser(), "Leave request approved",
                        "Your leave request has been approved.", NotificationType.LEAVE);
            }
            auditLogService.log(actor, "LEAVE_APPROVED", "LEAVE", "Approved leave request #" + leaveRequest.getId());
        } else {
            leaveRequest.setStatus(LeaveStatus.REJECTED);
            leaveRequest = leaveRequestRepository.save(leaveRequest);
            if (employee.getUser() != null) {
                notificationService.notify(employee.getUser(), "Leave request rejected",
                        "Your leave request was rejected: " + request.getComment(), NotificationType.LEAVE);
            }
            auditLogService.log(actor, "LEAVE_REJECTED", "LEAVE", "Rejected leave request #" + leaveRequest.getId());
        }

        return toResponse(leaveRequest);
    }

    // =========================================================================================
    // Internal helpers
    // =========================================================================================

    private LeaveRequest findEntity(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
    }

    private Employee requireOwnEmployee(User actor) {
        return employeeRepository.findByUser(actor)
                .orElseThrow(() -> new InvalidRequestException("No employee record is linked to your account. Contact an administrator."));
    }

    private LeaveRequestResponse toResponse(LeaveRequest leaveRequest) {
        Employee employee = leaveRequest.getEmployee();
        Optional<User> reviewer = Optional.ofNullable(leaveRequest.getReviewedBy());

        return LeaveRequestResponse.builder()
                .id(leaveRequest.getId())
                .employee(EmployeeSummary.builder()
                        .id(employee.getId())
                        .employeeId(employee.getEmployeeId())
                        .fullName(employee.getFirstName() + " " + employee.getLastName())
                        .designation(employee.getDesignation())
                        .build())
                .leaveType(leaveRequest.getLeaveType().name())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .numberOfDays(leaveRequest.getNumberOfDays())
                .reason(leaveRequest.getReason())
                .status(leaveRequest.getStatus().name())
                .appliedAt(leaveRequest.getAppliedAt())
                .reviewedAt(leaveRequest.getReviewedAt())
                .reviewedBy(reviewer.map(u -> UserSummary.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .role(u.getRole().name())
                        .fullName(null)
                        .build()).orElse(null))
                .reviewerComment(leaveRequest.getReviewerComment())
                .build();
    }
}
