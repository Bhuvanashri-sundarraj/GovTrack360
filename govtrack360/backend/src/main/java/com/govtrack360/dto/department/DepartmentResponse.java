package com.govtrack360.dto.department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DepartmentResponse {
    private Long id;
    private String departmentCode;
    private String departmentName;
    private String description;
    private String status;
    private long employeeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
