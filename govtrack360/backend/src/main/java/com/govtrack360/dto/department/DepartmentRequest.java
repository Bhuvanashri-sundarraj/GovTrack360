package com.govtrack360.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentRequest {

    @NotBlank(message = "Department code is required")
    @Size(max = 20, message = "Department code must be at most 20 characters")
    private String departmentCode;

    @NotBlank(message = "Department name is required")
    @Size(max = 150, message = "Department name must be at most 150 characters")
    private String departmentName;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;
}
