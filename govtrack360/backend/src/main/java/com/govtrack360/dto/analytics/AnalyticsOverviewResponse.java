package com.govtrack360.dto.analytics;

import com.govtrack360.dto.reward.RewardResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AnalyticsOverviewResponse {
    private LocalDate from;
    private LocalDate to;
    private String scope;
    private Kpis kpis;
    private List<DepartmentStat> departments;
    private List<Slice> taskStatusDistribution;
    private List<Slice> taskPriorityDistribution;
    private List<Slice> ratingBandDistribution;
    private List<RankedEmployee> topPerformers;
    private List<RankedEmployee> needsAttention;
    private List<TrendPoint> scoreTrend;
    private List<RewardResponse> recentRewards;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Kpis {
        private int employeesEvaluated;
        private Double averageScore;
        private Double taskCompletionRate;
        private Double onTimeRate;
        private Double attendanceRate;
        private int tasksInPeriod;
        private int overdueTasks;
        private long rewardsGiven;
        private long rewardPoints;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DepartmentStat {
        private Long departmentId;
        private String departmentName;
        private int employees;
        private Double averageScore;
        private Double completionRate;
        private Double attendanceRate;
        private long rewardPoints;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Slice {
        private String key;
        private String label;
        private long value;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RankedEmployee {
        private int rank;
        private Long employeeId;
        private String employeeCode;
        private String fullName;
        private String departmentName;
        private Double score;
        private String ratingBand;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TrendPoint {
        private String label;
        private Double averageScore;
        private Double completionRate;
        private Double attendanceRate;
    }
}
