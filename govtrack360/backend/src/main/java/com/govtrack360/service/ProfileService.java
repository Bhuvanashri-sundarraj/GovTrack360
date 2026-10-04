package com.govtrack360.service;

import com.govtrack360.dto.profile.ProfileResponse;
import com.govtrack360.dto.profile.ProfileUpdateRequest;
import com.govtrack360.entity.Employee;
import com.govtrack360.entity.User;
import com.govtrack360.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(User user) {
        Optional<Employee> employeeOpt = employeeRepository.findByUser(user);
        return toResponse(user, employeeOpt.orElse(null));
    }

    @Transactional
    public ProfileResponse updateProfile(User user, ProfileUpdateRequest request) {
        Employee employee = employeeRepository.findByUser(user).orElse(null);

        if (employee != null) {
            if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
                employee.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null && !request.getLastName().isBlank()) {
                employee.setLastName(request.getLastName());
            }
            if (request.getPhone() != null) {
                employee.setPhone(request.getPhone());
            }
            employeeRepository.save(employee);
        }

        return toResponse(user, employee);
    }

    private ProfileResponse toResponse(User user, Employee employee) {
        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .accountEnabled(user.isEnabled());

        if (employee != null) {
            builder.employeeRecordId(employee.getId())
                    .employeeId(employee.getEmployeeId())
                    .firstName(employee.getFirstName())
                    .lastName(employee.getLastName())
                    .phone(employee.getPhone())
                    .designation(employee.getDesignation())
                    .departmentName(employee.getDepartment().getDepartmentName())
                    .joiningDate(employee.getJoiningDate())
                    .employmentStatus(employee.getStatus().name());
        }

        return builder.build();
    }
}
