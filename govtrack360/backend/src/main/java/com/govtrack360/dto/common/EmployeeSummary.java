package com.govtrack360.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class EmployeeSummary {
    private Long id;
    private String employeeId;
    private String fullName;
    private String designation;
}
