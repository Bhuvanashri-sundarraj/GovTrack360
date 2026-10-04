package com.govtrack360.service;

import com.govtrack360.dto.department.DepartmentRequest;
import com.govtrack360.dto.department.DepartmentResponse;
import com.govtrack360.entity.Department;
import com.govtrack360.entity.Status;
import com.govtrack360.exception.DuplicateResourceException;
import com.govtrack360.exception.InvalidRequestException;
import com.govtrack360.exception.ResourceNotFoundException;
import com.govtrack360.repository.DepartmentRepository;
import com.govtrack360.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAll(String search) {
        List<Department> departments = (search == null || search.isBlank())
                ? departmentRepository.findAll()
                : departmentRepository.findByDepartmentNameContainingIgnoreCase(search);

        return departments.stream()
                .sorted(Comparator.comparing(Department::getDepartmentName))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByDepartmentCode(request.getDepartmentCode())) {
            throw new DuplicateResourceException("A department with this code already exists.");
        }

        Department department = Department.builder()
                .departmentCode(request.getDepartmentCode().toUpperCase())
                .departmentName(request.getDepartmentName())
                .description(request.getDescription())
                .status(Status.ACTIVE)
                .build();

        return toResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = findEntity(id);

        if (!department.getDepartmentCode().equalsIgnoreCase(request.getDepartmentCode())
                && departmentRepository.existsByDepartmentCode(request.getDepartmentCode())) {
            throw new DuplicateResourceException("A department with this code already exists.");
        }

        department.setDepartmentCode(request.getDepartmentCode().toUpperCase());
        department.setDepartmentName(request.getDepartmentName());
        department.setDescription(request.getDescription());

        return toResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse updateStatus(Long id, String statusValue) {
        Department department = findEntity(id);
        Status status = parseStatus(statusValue);
        department.setStatus(status);
        return toResponse(departmentRepository.save(department));
    }

    @Transactional
    public void delete(Long id) {
        Department department = findEntity(id);
        long employeeCount = employeeRepository.countByDepartmentId(id);
        if (employeeCount > 0) {
            throw new InvalidRequestException(
                    "Cannot delete a department that still has " + employeeCount + " employee(s) assigned. " +
                            "Reassign or remove those employees first, or deactivate the department instead.");
        }
        departmentRepository.delete(department);
    }

    private Department findEntity(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    private Status parseStatus(String value) {
        try {
            return Status.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new InvalidRequestException("Invalid status value. Use ACTIVE or INACTIVE.");
        }
    }

    private DepartmentResponse toResponse(Department d) {
        long count = employeeRepository.countByDepartmentId(d.getId());
        return DepartmentResponse.builder()
                .id(d.getId())
                .departmentCode(d.getDepartmentCode())
                .departmentName(d.getDepartmentName())
                .description(d.getDescription())
                .status(d.getStatus().name())
                .employeeCount(count)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
