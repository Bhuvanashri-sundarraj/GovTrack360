package com.govtrack360.dto.leave;

import com.govtrack360.dto.common.EmployeeSummary;
import com.govtrack360.dto.common.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class LeaveRequestResponse {
    private Long id;
    private EmployeeSummary employee;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int numberOfDays;
    private String reason;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private UserSummary reviewedBy;
    private String reviewerComment;
}
