package com.govtrack360.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AttendanceMonthlySummaryResponse {
    private int month;
    private int year;
    private long presentDays;
    private long lateDays;
    private long halfDays;
    private long leaveDays;
    private long absentDays;
    private long totalRecordedDays;
}
