package com.govtrack360.dto.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * An active employee the caller is allowed to review or recognise (never the caller themself).
 */
@Getter
@Builder
@AllArgsConstructor
public class EligibleEmployeeResponse {
    private Long id;
    private String employeeId;
    private String fullName;
    private String designation;
    private Long departmentId;
    private String departmentName;
}
