package com.govtrack360.dto.employee;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EmployeeRequest {

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String phone;

    @NotBlank(message = "Designation is required")
    private String designation;

    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotNull(message = "Joining date is required")
    @PastOrPresent(message = "Joining date cannot be in the future")
    private LocalDate joiningDate;

    /**
     * Optional. If provided, a login account (User) is created for this employee
     * with this initial password. If omitted, no login account is created.
     */
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String initialPassword;

    /**
     * Optional. Only used together with initialPassword when creating a login account.
     */
    private String role;
}
