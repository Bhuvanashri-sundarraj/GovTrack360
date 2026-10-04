package com.govtrack360.dto.department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DepartmentSummary {
    private Long id;
    private String departmentCode;
    private String departmentName;
    private String status;
}
