package com.govtrack360.dto.reward;

import com.govtrack360.dto.common.EmployeeSummary;
import com.govtrack360.dto.common.UserSummary;
import com.govtrack360.dto.department.DepartmentSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class RewardResponse {
    private Long id;
    private EmployeeSummary employee;
    private DepartmentSummary department;
    private UserSummary awardedBy;
    private String type;
    private String typeLabel;
    private String title;
    private String citation;
    private int points;
    private LocalDate awardedDate;
    private LocalDateTime createdAt;
}
