package com.govtrack360.dto.reward;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MyRewardsResponse {
    private long totalPoints;
    private long totalRewards;
    private List<RewardResponse> rewards;
}
