package com.govtrack360.dto.attendance;

import com.govtrack360.dto.common.EmployeeSummary;
import com.govtrack360.dto.department.DepartmentSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
@AllArgsConstructor
public class AttendanceResponse {
    private Long id;
    private EmployeeSummary employee;
    private DepartmentSummary department;
    private LocalDate date;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private Double totalWorkingHours;
    private String status;
}
