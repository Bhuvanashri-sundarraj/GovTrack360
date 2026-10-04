package com.govtrack360.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TodayAttendanceSummaryResponse {
    private long totalEmployees;
    private long presentToday;
    private long absentToday;
    private long onLeaveToday;
    private long lateToday;
}
