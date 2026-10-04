package com.govtrack360.service;

import com.govtrack360.dto.department.DepartmentSummary;
import com.govtrack360.dto.employee.EmployeeRequest;
import com.govtrack360.dto.employee.EmployeeResponse;
import com.govtrack360.entity.*;
import com.govtrack360.exception.DuplicateResourceException;
import com.govtrack360.exception.InvalidRequestException;
import com.govtrack360.exception.ResourceNotFoundException;
import com.govtrack360.repository.DepartmentRepository;
import com.govtrack360.repository.EmployeeRepository;
import com.govtrack360.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<EmployeeResponse> search(String search, Long departmentId, String statusValue) {
        Status status = null;
        if (statusValue != null && !statusValue.isBlank()) {
            status = parseStatus(statusValue);
        }
        // An empty string (not null) matches everything; a null String bound inside LOWER() makes PostgreSQL fail
        // with "function lower(bytea) does not exist".
        String normalizedSearch = (search == null || search.isBlank()) ? "" : search.trim();
        return employeeRepository.search(normalizedSearch, departmentId, status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (employeeRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new DuplicateResourceException("An employee with this Employee ID already exists.");
        }
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An employee with this email already exists.");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        Employee employee = Employee.builder()
                .employeeId(request.getEmployeeId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .designation(request.getDesignation())
                .department(department)
                .joiningDate(request.getJoiningDate())
                .status(Status.ACTIVE)
                .build();

        if (request.getInitialPassword() != null && !request.getInitialPassword().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("A login account with this email already exists.");
            }
            Role role = parseRole(request.getRole());
            User user = User.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getInitialPassword()))
                    .role(role)
                    .enabled(true)
                    .build();
            employee.setUser(userRepository.save(user));
        }

        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findEntity(id);

        if (!employee.getEmployeeId().equalsIgnoreCase(request.getEmployeeId())
                && employeeRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new DuplicateResourceException("An employee with this Employee ID already exists.");
        }
        if (!employee.getEmail().equalsIgnoreCase(request.getEmail())
                && employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An employee with this email already exists.");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        employee.setEmployeeId(request.getEmployeeId());
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setDesignation(request.getDesignation());
        employee.setDepartment(department);
        employee.setJoiningDate(request.getJoiningDate());

        if (employee.getUser() != null) {
            employee.getUser().setEmail(request.getEmail());
            userRepository.save(employee.getUser());
        }

        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse updateStatus(Long id, String statusValue) {
        Employee employee = findEntity(id);
        Status status = parseStatus(statusValue);
        employee.setStatus(status);

        if (employee.getUser() != null) {
            employee.getUser().setEnabled(status == Status.ACTIVE);
            userRepository.save(employee.getUser());
        }

        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = findEntity(id);
        User linkedUser = employee.getUser();
        employeeRepository.delete(employee);
        if (linkedUser != null) {
            userRepository.delete(linkedUser);
        }
    }

    private Employee findEntity(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    private Status parseStatus(String value) {
        try {
            return Status.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new InvalidRequestException("Invalid status value. Use ACTIVE or INACTIVE.");
        }
    }

    private Role parseRole(String value) {
        if (value == null || value.isBlank()) {
            return Role.EMPLOYEE;
        }
        try {
            return Role.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new InvalidRequestException("Invalid role value. Use ADMIN, OFFICER or EMPLOYEE.");
        }
    }

    private EmployeeResponse toResponse(Employee e) {
        DepartmentSummary deptSummary = DepartmentSummary.builder()
                .id(e.getDepartment().getId())
                .departmentCode(e.getDepartment().getDepartmentCode())
                .departmentName(e.getDepartment().getDepartmentName())
                .status(e.getDepartment().getStatus().name())
                .build();

        return EmployeeResponse.builder()
                .id(e.getId())
                .employeeId(e.getEmployeeId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .fullName(e.getFirstName() + " " + e.getLastName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .designation(e.getDesignation())
                .department(deptSummary)
                .joiningDate(e.getJoiningDate())
                .status(e.getStatus().name())
                .hasLoginAccount(e.getUser() != null)
                .role(e.getUser() != null ? e.getUser().getRole().name() : null)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
