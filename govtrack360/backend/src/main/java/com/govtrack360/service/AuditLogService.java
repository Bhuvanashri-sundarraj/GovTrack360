package com.govtrack360.service;

import com.govtrack360.dto.audit.AuditLogResponse;
import com.govtrack360.dto.common.UserSummary;
import com.govtrack360.entity.AuditLog;
import com.govtrack360.entity.Employee;
import com.govtrack360.entity.Role;
import com.govtrack360.entity.User;
import com.govtrack360.repository.AuditLogRepository;
import com.govtrack360.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Audit log API. Creation is used internally by other services throughout the application.
 * Audit logs are append-only: no update/delete method exists anywhere in this service.
 * Reading is restricted to ADMIN at the controller layer via @PreAuthorize.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public void log(User user, String action, String module, String description) {
        Role role = (user != null) ? user.getRole() : null;
        AuditLog entry = AuditLog.builder()
                .user(user)
                .role(role)
                .action(action)
                .module(module)
                .description(description)
                .build();
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> search(String search, Long userId, String module, String action,
                                          LocalDateTime from, LocalDateTime to) {
        String normalizedSearch = (search == null || search.isBlank()) ? "" : search.trim(); // "" not null: see EmployeeService
        String normalizedModule = (module == null || module.isBlank()) ? null : module.trim().toUpperCase();
        String normalizedAction = (action == null || action.isBlank()) ? null : action.trim().toUpperCase();

        return auditLogRepository.search(normalizedSearch, userId, normalizedModule, normalizedAction, from, to)
                .stream().map(this::toResponse).toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        UserSummary userSummary = null;
        if (log.getUser() != null) {
            User user = log.getUser();
            Optional<Employee> employee = employeeRepository.findByUser(user);
            userSummary = UserSummary.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .fullName(employee.map(e -> e.getFirstName() + " " + e.getLastName()).orElse(null))
                    .build();
        }

        return AuditLogResponse.builder()
                .id(log.getId())
                .user(userSummary)
                .role(log.getRole() != null ? log.getRole().name() : null)
                .action(log.getAction())
                .module(log.getModule())
                .description(log.getDescription())
                .timestamp(log.getTimestamp())
                .build();
    }
}
