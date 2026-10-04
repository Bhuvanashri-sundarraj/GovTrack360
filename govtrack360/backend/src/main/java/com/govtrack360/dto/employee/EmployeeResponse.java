package com.govtrack360.dto.employee;

import com.govtrack360.dto.department.DepartmentSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String designation;
    private DepartmentSummary department;
    private LocalDate joiningDate;
    private String status;
    private boolean hasLoginAccount;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
