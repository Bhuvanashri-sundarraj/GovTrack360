package com.govtrack360.service;

import com.govtrack360.dto.common.EmployeeSummary;
import com.govtrack360.dto.common.UserSummary;
import com.govtrack360.dto.department.DepartmentSummary;
import com.govtrack360.entity.Department;
import com.govtrack360.entity.Employee;
import com.govtrack360.entity.User;
import com.govtrack360.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Small shared mappers used by the Phase 3 services. Callers must be inside a transaction because the
 * entities' lazy associations are read here.
 */
@Component
@RequiredArgsConstructor
public class SummaryMapper {

    private final EmployeeRepository employeeRepository;

    public EmployeeSummary employee(Employee employee) {
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

    public DepartmentSummary department(Department department) {
        if (department == null) {
            return null;
        }
        return DepartmentSummary.builder()
                .id(department.getId())
                .departmentCode(department.getDepartmentCode())
                .departmentName(department.getDepartmentName())
                .status(department.getStatus().name())
                .build();
    }

    public UserSummary user(User user) {
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

    public String fullName(Employee employee) {
        return employee.getFirstName() + " " + employee.getLastName();
    }
}
