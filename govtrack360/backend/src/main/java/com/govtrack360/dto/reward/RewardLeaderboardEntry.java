package com.govtrack360.dto.reward;

import com.govtrack360.dto.common.EmployeeSummary;
import com.govtrack360.dto.department.DepartmentSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RewardLeaderboardEntry {
    private int rank;
    private EmployeeSummary employee;
    private DepartmentSummary department;
    private long totalPoints;
    private long rewardCount;
}
