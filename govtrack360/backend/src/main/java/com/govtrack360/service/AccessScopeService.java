package com.govtrack360.service;

import com.govtrack360.entity.Employee;
import com.govtrack360.entity.Role;
import com.govtrack360.entity.Status;
import com.govtrack360.entity.User;
import com.govtrack360.exception.InvalidRequestException;
import com.govtrack360.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Central place for the Phase 3 role-scoping rules, mirroring what TaskService, AttendanceService and
 * LeaveService already enforce: ADMIN sees everything, OFFICER is limited to their own department.
 */
@Service
@RequiredArgsConstructor
public class AccessScopeService {

    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public Optional<Employee> findOwnEmployee(User actor) {
        return employeeRepository.findByUser(actor);
    }

    @Transactional(readOnly = true)
    public Employee requireOwnEmployee(User actor) {
        return employeeRepository.findByUser(actor)
                .orElseThrow(() -> new InvalidRequestException(
                        "No employee record is linked to your account. Contact an administrator."));
    }

    /**
     * Resolves the department an ADMIN/OFFICER request should be limited to.
     * ADMIN: whatever was requested (null = all departments).
     * OFFICER: always their own department; asking for a different one is rejected.
     */
    @Transactional(readOnly = true)
    public Long resolveDepartmentScope(User actor, Long requestedDepartmentId) {
        if (actor.getRole() == Role.ADMIN) {
            return requestedDepartmentId;
        }
        if (actor.getRole() == Role.OFFICER) {
            Employee officerEmployee = employeeRepository.findByUser(actor)
                    .orElseThrow(() -> new AccessDeniedException("No department is linked to your account."));
            Long ownDepartmentId = officerEmployee.getDepartment().getId();
            if (requestedDepartmentId != null && !requestedDepartmentId.equals(ownDepartmentId)) {
                throw new AccessDeniedException("Officers may only access data within their own department.");
            }
            return ownDepartmentId;
        }
        throw new AccessDeniedException("You do not have permission to perform this action.");
    }

    /**
     * ADMIN: any employee. OFFICER: employees in their own department. EMPLOYEE: only themself.
     */
    @Transactional(readOnly = true)
    public void assertCanAccessEmployee(User actor, Employee target) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        Employee own = employeeRepository.findByUser(actor).orElse(null);
        if (own == null) {
            throw new AccessDeniedException("You do not have access to this employee's data.");
        }
        if (actor.getRole() == Role.OFFICER) {
            if (own.getDepartment().getId().equals(target.getDepartment().getId())) {
                return;
            }
            throw new AccessDeniedException("Officers may only access employees within their own department.");
        }
        if (own.getId().equals(target.getId())) {
            return;
        }
        throw new AccessDeniedException("You do not have access to this employee's data.");
    }

    /**
     * Active employees the actor may see, optionally narrowed to one department.
     */
    @Transactional(readOnly = true)
    public List<Employee> activeEmployeesInScope(User actor, Long requestedDepartmentId) {
        Long departmentId = resolveDepartmentScope(actor, requestedDepartmentId);
        return employeeRepository.findActive(departmentId);
    }
}
