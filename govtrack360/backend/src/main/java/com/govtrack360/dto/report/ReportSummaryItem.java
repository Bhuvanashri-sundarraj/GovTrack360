package com.govtrack360.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReportSummaryItem {
    private String label;
    private String value;
}
