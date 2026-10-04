package com.govtrack360.dto.reward;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RewardTypeResponse {
    private String value;
    private String label;
    private int defaultPoints;
}
